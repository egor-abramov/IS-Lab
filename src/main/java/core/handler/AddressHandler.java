package core.handler;

import core.dto.AddressDto;
import core.dto.AddressRequest;
import core.dto.ItemsDto;
import core.service.AddressService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/addresses")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressHandler {
    @Inject
    AddressService service;

    @GET
    public ItemsDto<AddressDto> list() {
        return service.list();
    }

    @POST
    public Response create(AddressRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.create(request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id, @QueryParam("replaceWith") Long replaceWith) {
        service.delete(id, replaceWith);
        return Response.noContent().build();
    }
}
