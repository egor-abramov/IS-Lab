package core.service;

import core.dto.AuthDto;
import core.dto.CredentialsRequest;
import core.dto.UserDto;
import core.model.AppUser;
import core.repository.UserRepository;
import core.security.JwtService;
import core.security.PasswordHasher;
import core.service.exception.DataConflictException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
@Transactional
public class AuthService {
    @Inject
    UserRepository repository;
    @Inject
    DataValidator validator;
    @Inject
    PasswordHasher passwords;
    @Inject
    JwtService jwt;

    public AuthDto register(CredentialsRequest request) {
        validator.check(request);
        if (repository.findByLogin(request.login()) != null) {
            throw new DataConflictException("логин уже занят");
        }
        AppUser user = new AppUser();
        user.setLogin(request.login());
        user.setPasswordHash(passwords.hash(request.password()));
        repository.create(user);
        return new AuthDto(jwt.issue(user), UserDto.from(user));
    }

    public AuthDto login(CredentialsRequest request) {
        validator.check(request);
        AppUser user = repository.findByLogin(request.login());
        if (user == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new NotAuthorizedException("неверный логин или пароль", "Bearer");
        }
        return new AuthDto(jwt.issue(user), UserDto.from(user));
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public AppUser authenticate(String token) {
        AppUser user = repository.findById(jwt.userId(token));
        if (user == null) {
            throw new NotAuthorizedException("пользователь не найден", "Bearer");
        }
        return user;
    }
}
