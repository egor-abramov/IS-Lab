package core.handler;

import core.dto.CoordinatesDto;
import core.dto.CoordinatesRequest;
import core.dto.ItemsDto;
import core.service.CoordinatesService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/coordinates")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoordinatesHandler {
    @Inject
    CoordinatesService service;

    @GET
    public ItemsDto<CoordinatesDto> list() {
        return service.list();
    }

    @POST
    public Response create(CoordinatesRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.create(request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id, @QueryParam("replaceWith") Long replaceWith) {
        service.delete(id, replaceWith);
        return Response.noContent().build();
    }
}
