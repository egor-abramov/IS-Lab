package core.repository;

import core.model.*;
import core.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "postgresTests", matches = "true")
class OwnershipDatabaseTest {
    static Connection connection;
    static EntityManagerFactory factory;
    static String schema;
    EntityManager em;
    CurrentUser current;
    OrganizationRepository organizations;
    CoordinatesRepository coordinates;
    SpecialOpsRepository special;
    AppUser alice;
    AppUser bob;
    AppUser admin;
    Organization first;
    Organization second;

    @BeforeAll
    static void database() throws Exception {
        Properties config = new Properties();
        try (var input = Files.newInputStream(Path.of("config/local.properties"))) {
            config.load(input);
        }
        connection = DriverManager.getConnection(config.getProperty("db.url"),
                config.getProperty("db.user"), config.getProperty("db.password"));
        schema = "auth_test_" + UUID.randomUUID().toString().replace("-", "");
        connection.createStatement().execute("create schema " + schema);
        connection.setSchema(schema);
        for (int run = 0; run < 2; run++) {
            connection.createStatement().execute(Files.readString(Path.of("sql/schema.sql")));
            connection.createStatement().execute(Files.readString(Path.of("sql/functions.sql")));
        }
        String url = config.getProperty("db.url");
        url += (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        factory = Persistence.createEntityManagerFactory("auth-test", Map.of(
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", config.getProperty("db.user"),
                "jakarta.persistence.jdbc.password", config.getProperty("db.password"),
                "jakarta.persistence.jdbc.driver", "org.postgresql.Driver"));
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (factory != null) factory.close();
        if (connection != null) {
            if (schema != null && schema.matches("auth_test_[a-f0-9]{32}")) {
                connection.createStatement().execute("drop schema if exists " + schema + " cascade");
            }
            connection.close();
        }
    }

    @BeforeEach
    void setup() {
        em = factory.createEntityManager();
        em.getTransaction().begin();
        alice = user("alice", Role.USER);
        bob = user("bob", Role.USER);
        admin = user("admin", Role.ADMIN);
        first = organization(alice.getId(), "First", 10, 100f);
        second = organization(bob.getId(), "Second", 20, 200f);
        organization(null, "Legacy", 30, 300f);
        em.flush();
        current = new CurrentUser();
        current.set(alice);
        organizations = new OrganizationRepository();
        coordinates = new CoordinatesRepository();
        special = new SpecialOpsRepository();
        for (BaseRepository repository : new BaseRepository[]{organizations, coordinates, special}) {
            repository.em = em;
            repository.currentUser = current;
        }
    }

    @AfterEach
    void rollback() {
        if (em != null) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    @Test
    void listsIdsAndCountsAreScopedBeforePagination() {
        assertEquals(1, organizations.count(null, null));
        assertEquals(first.getId(), organizations.findPage(null, null, "id", false, 0, 1).get(0).getId());
        assertTrue(organizations.findPage(null, null, "id", true, 1, 1).isEmpty());
        assertNull(organizations.findById(second.getId()));
        assertFalse(organizations.delete(second.getId()));
        assertEquals(1, coordinates.findAll().size());
        current.set(admin);
        assertEquals(3, organizations.count(null, null));
        assertNotNull(organizations.findById(second.getId()));
        assertEquals(3, coordinates.findAll().size());
        assertThrows(ForbiddenException.class, () -> organizations.delete(second.getId()));
        assertThrows(ForbiddenException.class, () -> organizations.save(first));
    }

    @Test
    void allSpecialFunctionsUseTheCurrentOwner() {
        assertEquals(1, special.countFullNameGreater(""));
        assertEquals(1, special.findNameContains("i").size());
        assertEquals(java.util.List.of(10L), special.uniqueRatings());
        assertEquals(first.getId(), special.top5ByTurnover().get(0).getId());
        assertEquals(10, special.avgEmployeesTop10());
        current.set(bob);
        assertEquals(second.getId(), special.top5ByTurnover().get(0).getId());
        assertEquals(20, special.avgEmployeesTop10());
        current.set(admin);
        assertEquals(3, special.countFullNameGreater(""));
        assertEquals(3, special.top5ByTurnover().size());
        assertEquals(20, special.avgEmployeesTop10());
    }

    @Test
    void newRecordsGetServerAssignedOwnership() {
        Coordinates coordinate = new Coordinates();
        coordinate.setOwnerId(bob.getId());
        coordinate.setX(1);
        coordinate.setY(2);
        Coordinates saved = coordinates.save(coordinate);
        assertEquals(alice.getId(), saved.getOwnerId());
        current.set(bob);
        assertNull(coordinates.findById(saved.getId()));
    }

    private AppUser user(String login, Role role) {
        AppUser user = new AppUser();
        user.setLogin(login);
        user.setPasswordHash("unused");
        user.setRole(role);
        em.persist(user);
        em.flush();
        return user;
    }

    private Organization organization(Long owner, String name, int employees, float turnover) {
        Coordinates coordinate = new Coordinates();
        coordinate.setOwnerId(owner);
        coordinate.setX(1);
        coordinate.setY(2);
        Address address = new Address();
        address.setOwnerId(owner);
        address.setStreet("Street");
        Organization organization = new Organization();
        organization.setOwnerId(owner);
        organization.setName(name);
        organization.setFullName(name);
        organization.setEmployeesCount(employees);
        organization.setAnnualTurnover(turnover);
        organization.setRating((long) employees);
        organization.setCoordinates(coordinate);
        organization.setPostalAddress(address);
        em.persist(organization);
        return organization;
    }
}
