package platform.business.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import platform.business.api.dto.BusinessResponse;
import platform.business.api.dto.CreateBusinessRequest;
import platform.business.application.BusinessOnboardingService;

@Path("/api/admin/businesses")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class BusinessResource {
  private final BusinessOnboardingService onboarding;

  @Inject
  public BusinessResource(BusinessOnboardingService onboarding) {
    this.onboarding = onboarding;
  }

  @POST
  public Response create(@NotNull @Valid CreateBusinessRequest request) {
    var business = onboarding.register(request.name(), request.ownerKeycloakUserId());
    return Response.status(Response.Status.CREATED)
      .entity(BusinessResponse.from(business))
      .build();
  }
}
