package core.handler;

import core.dto.LocationDto;
import core.dto.LocationRequest;
import core.dto.ItemsDto;
import core.service.LocationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/locations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationHandler {
    @Inject
    LocationService service;

    @GET
    public ItemsDto<LocationDto> list() {
        return service.list();
    }

    @POST
    public Response create(LocationRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.create(request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id, @QueryParam("replaceWith") Long replaceWith) {
        service.delete(id, replaceWith);
        return Response.noContent().build();
    }
}
