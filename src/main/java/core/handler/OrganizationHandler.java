package core.handler;

import core.dto.OrganizationDto;
import core.dto.OrganizationPageDto;
import core.dto.OrganizationRequest;
import core.model.Organization;
import core.model.OrganizationSource;
import core.service.OrganizationService;
import core.service.OrganizationImportService;
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
    @Inject
    OrganizationService service;
    @Inject
    OrganizationImportService imports;

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
        imports.load(body);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/list")
    public OrganizationPageDto loaded(@QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("5") int size
                           ) {
        return service.list(page, size, "source", OrganizationSource.FILE.toString(), null, null);
    }
}
