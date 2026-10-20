package restaurant.overview.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import restaurant.overview.application.OverviewService;
import restaurant.overview.application.OverviewSummary;

@Path("/api/overview")
@RolesAllowed({"owner", "waiter"})
@Produces(MediaType.APPLICATION_JSON)
public class OverviewResource {
  private final OverviewService service;

  @Inject
  public OverviewResource(OverviewService service) {
    this.service = service;
  }

  @GET
  public OverviewSummary get() {
    return service.get();
  }
}
