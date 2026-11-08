package core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import core.model.ImportStatus;
import core.model.Organization;
import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OrganizationImportService {
    private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    @Inject
    OrganizationService organizations;
    @Inject
    ImportHistoryService history;
    @Inject
    SerializationRetry retry;

    public void load(String body) {
        long id = history.start();
        try {
            List<Organization> items = parse(body);

            retry.run(() -> {
                organizations.load(items);
                return null;
            });
            history.finish(id, ImportStatus.SUCCESS, items.size());
        } catch (Exception e) {
            history.finish(id, ImportStatus.FAILED, null);
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("ошибка сохранения импорта", e);
        }
    }

    private List<Organization> parse(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidDataException("YAML-файл не должен быть пустым");
        }
        List<Organization> items;
        try {
            items = YAML_MAPPER.readValue(body, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new InvalidDataException("некорректный YAML: ожидается список организаций с корректными типами полей");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidDataException("список организаций не должен быть пустым");
        }
        return items;
    }
}
