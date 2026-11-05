package core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import core.dto.OrganizationDto;
import core.dto.OrganizationPageDto;
import core.dto.OrganizationRequest;
import core.model.Organization;
import core.service.OrganizationService;
import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/organizations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganizationHandler {
    private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    @Inject
    OrganizationService service;

    @GET
    public OrganizationPageDto list(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("5") int size,
                                    @QueryParam("filterField") String filterField,
                                    @QueryParam("filterValue") String filterValue,
                                    @QueryParam("sortField") String sortField,
                                    @QueryParam("sortDir") String sortDir) {
        return service.list(page, size, filterField, filterValue, sortField, sortDir);
    }

    @GET
    @Path("/{id}")
    public OrganizationDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(OrganizationRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.create(request)).build();
    }

    @PUT
    @Path("/{id}")
    public OrganizationDto update(@PathParam("id") long id, OrganizationRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/list")
    @Consumes("application/yaml")
    public Response load(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidDataException("YAML-файл не должен быть пустым");
        }

        List<Organization> organizations;
        try {
            organizations = YAML_MAPPER.readValue(body, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new InvalidDataException("некорректный YAML: ожидается список организаций с корректными типами полей");
        }
        if (organizations == null || organizations.isEmpty()) {
            throw new InvalidDataException("список организаций не должен быть пустым");
        }

        service.load(organizations);
        return Response.status(Response.Status.CREATED).build();
    }
}
