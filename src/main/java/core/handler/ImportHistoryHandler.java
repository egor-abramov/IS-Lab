package core.handler;

import core.dto.ImportOperationDto;
import core.dto.ItemsDto;
import core.service.ImportHistoryService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/imports")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImportHistoryHandler {
    @Inject
    ImportHistoryService history;

    @GET
    public ItemsDto<ImportOperationDto> list() {
        return history.history();
    }
}
