package core.security;

import core.model.AppUser;
import core.model.Organization;
import core.model.Role;
import io.jsonwebtoken.Jwts;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {
    @Test
    void passwordIsSaltedAndVerified() {
        PasswordHasher hasher = new PasswordHasher();
        String first = hasher.hash("test-password");
        String second = hasher.hash("test-password");
        assertNotEquals(first, second);
        assertFalse(first.contains("test-password"));
        assertTrue(hasher.matches("test-password", first));
        assertFalse(hasher.matches("wrong-password", first));
    }

    @Test
    void jwtRejectsTamperingExpirationAndMissingExpiration() throws Exception {
        JwtService jwt = new JwtService();
        jwt.init();
        AppUser user = new AppUser();
        user.setId(7L);
        String token = jwt.issue(user);
        assertEquals(7L, jwt.userId(token));
        String[] parts = token.split("\\.");
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(parts[0] + ".e30." + parts[2]));
        assertThrows(NotAuthorizedException.class, () -> jwt.userId("invalid"));
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(null));
        String forged = Jwts.builder().issuer("is-lab").subject("7")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(forged));
        Field field = JwtService.class.getDeclaredField("key");
        field.setAccessible(true);
        SecretKey key = (SecretKey) field.get(jwt);
        String expired = Jwts.builder().issuer("is-lab").subject("7")
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        String noExpiration = Jwts.builder().issuer("is-lab").subject("7").signWith(key).compact();
        String wrongIssuer = Jwts.builder().issuer("other").subject("7")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key).compact();
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(expired));
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(noExpiration));
        assertThrows(NotAuthorizedException.class, () -> jwt.userId(wrongIssuer));
    }

    @Test
    void adminCanReadButCannotWriteOtherUsersData() {
        CurrentUser current = new CurrentUser();
        AppUser user = new AppUser();
        user.setId(1L);
        current.set(user);
        Organization own = new Organization();
        own.setOwnerId(1L);
        Organization foreign = new Organization();
        foreign.setOwnerId(2L);
        Organization legacy = new Organization();
        assertTrue(current.canRead(own));
        assertFalse(current.canRead(foreign));
        assertFalse(current.canRead(legacy));
        assertThrows(ForbiddenException.class, () -> current.requireWrite(foreign));
        user.setRole(Role.ADMIN);
        assertTrue(current.canRead(foreign));
        assertTrue(current.canRead(legacy));
        assertDoesNotThrow(() -> current.requireWrite(own));
        assertThrows(ForbiddenException.class, () -> current.requireWrite(foreign));
        assertThrows(ForbiddenException.class, () -> current.requireWrite(legacy));
        assertThrows(ForbiddenException.class, () -> current.requireOwnReference(foreign));
    }
}
