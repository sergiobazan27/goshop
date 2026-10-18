package platform.business.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import platform.business.api.dto.BusinessResponse;
import platform.business.api.dto.CreateBusinessRequest;
import platform.business.application.BusinessOnboardingService;
import platform.business.application.BusinessService;
import platform.business.domain.BusinessStatus;
import shared.application.Page;

@Path("/api/admin/businesses")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class BusinessResource {
  private final BusinessOnboardingService onboarding;
  private final BusinessService businessService;

  @Inject
  public BusinessResource(BusinessOnboardingService onboarding, BusinessService businessService) {
    this.onboarding = onboarding;
    this.businessService = businessService;
  }

  @GET
  public Page<BusinessResponse> list(
    @QueryParam("search") @DefaultValue("") @Size(max = 150) String search,
    @QueryParam("status") @Pattern(regexp = "ACTIVE|SUSPENDED") String status,
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size
  ) {
    var result = businessService.list(
      search, status == null ? null : BusinessStatus.valueOf(status), page, size
    );
    return new Page<>(
      result.items().stream().map(BusinessResponse::from).toList(),
      result.page(), result.size(), result.total(), result.totalPages()
    );
  }

  @POST
  public Response create(@NotNull @Valid CreateBusinessRequest request) {
    var business = onboarding.register(request.name(), request.ownerKeycloakUserId());
    return Response.status(Response.Status.CREATED)
      .entity(BusinessResponse.from(business))
      .build();
  }
}
