package core.security;

import core.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@ApplicationScoped
public class JwtService {
    private static final String ISSUER = "is-lab";
    private SecretKey key;

    @PostConstruct
    public void init() {
        String secret = System.getenv("JWT_SECRET");
        key = secret == null || secret.isBlank()
                ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public String issue(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(ISSUER).subject(user.getId().toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public long userId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER)
                    .build().parseSignedClaims(token).getPayload();
            if (claims.getExpiration() == null) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(claims.getSubject());
            if (id <= 0) {
                throw new IllegalArgumentException();
            }
            return id;
        } catch (JwtException | IllegalArgumentException e) {
            throw new NotAuthorizedException("недействительный или просроченный токен", "Bearer");
        }
    }
}
