package core.repository;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ApplicationScoped
public class EmfProducer {
    private EntityManagerFactory emf;

    @Produces
    public synchronized EntityManagerFactory getEmf() {
        if (emf == null) {
            Properties config = loadConfig();
            Map<String, String> props = new HashMap<>();
            props.put("jakarta.persistence.jdbc.url", config.getProperty("db.url"));
            props.put("jakarta.persistence.jdbc.user", config.getProperty("db.user"));
            props.put("jakarta.persistence.jdbc.password", config.getProperty("db.password"));
            emf = Persistence.createEntityManagerFactory("lab1", props);
        }
        return emf;
    }

    private Properties loadConfig() {
        try (InputStream in = EmfProducer.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found, build the war with -Pprofile=<name>");
            }
            Properties config = new Properties();
            config.load(in);
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read db.properties", e);
        }
    }

    @PreDestroy
    void close() {
        if (emf != null) {
            emf.close();
        }
    }
}
