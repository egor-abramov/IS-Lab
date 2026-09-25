package core.handler;

import core.dto.AverageDto;
import core.dto.CountDto;
import core.dto.ItemsDto;
import core.dto.OrganizationDto;
import core.dto.RatingsDto;
import core.service.SpecialOpsService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/special")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SpecialOpsHandler {
    @Inject
    SpecialOpsService service;

    @GET
    @Path("/count-full-name-greater")
    public CountDto countFullNameGreater(@QueryParam("value") String value) {
        return service.countFullNameGreater(value);
    }

    @GET
    @Path("/name-contains")
    public ItemsDto<OrganizationDto> nameContains(@QueryParam("value") String value) {
        return service.findNameContains(value);
    }

    @GET
    @Path("/unique-ratings")
    public RatingsDto uniqueRatings() {
        return service.uniqueRatings();
    }

    @GET
    @Path("/top5-turnover")
    public ItemsDto<OrganizationDto> top5ByTurnover() {
        return service.top5ByTurnover();
    }

    @GET
    @Path("/avg-employees-top10")
    public AverageDto avgEmployeesTop10() {
        return service.avgEmployeesTop10();
    }
}
