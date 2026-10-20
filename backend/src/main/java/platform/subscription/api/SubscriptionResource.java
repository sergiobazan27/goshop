package platform.subscription.api;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import platform.business.application.BusinessService;
import platform.subscription.api.dto.AssignSubscriptionRequest;
import platform.subscription.api.dto.RenewSubscriptionRequest;
import platform.subscription.api.dto.ChangeSubscriptionPlanRequest;
import platform.subscription.application.SubscriptionService;
import platform.subscription.application.SubscriptionSummary;
import platform.subscription.application.SubscriptionHistoryEntry;
import shared.application.Page;

@Path("/api/admin/businesses/{businessId}/subscription")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubscriptionResource {
  private final SubscriptionService service;
  private final BusinessService businesses;

  @Inject
  public SubscriptionResource(SubscriptionService service, BusinessService businesses) {
    this.service = service; this.businesses = businesses;
  }

  @GET
  public Response current(@PathParam("businessId") UUID businessId) {
    businesses.requireExisting(businessId);
    return service.findByBusinessId(businessId).map(s -> Response.ok(s).build())
      .orElseGet(() -> Response.noContent().build());
  }

  @POST
  public Response assign(@PathParam("businessId") UUID businessId,
    @NotNull @Valid AssignSubscriptionRequest request) {
    return Response.status(Response.Status.CREATED).entity(service.assign(businessId,
      request.planId(), request.startsAt(), request.expiresAt())).build();
  }

  @POST
  @Path("/renewals")
  public SubscriptionSummary renew(@PathParam("businessId") UUID businessId,
    @NotNull @Valid RenewSubscriptionRequest request) {
    return service.renew(businessId, request.planId(), request.startsAt(), request.expiresAt());
  }

  @PUT
  @Path("/plan")
  public SubscriptionSummary changePlan(@PathParam("businessId") UUID businessId,
    @NotNull @Valid ChangeSubscriptionPlanRequest request) {
    return service.changePlan(businessId, request.planId());
  }

  @GET
  @Path("/history")
  public Page<SubscriptionHistoryEntry> history(@PathParam("businessId") UUID businessId,
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    businesses.requireExisting(businessId);
    return service.history(businessId, page, size);
  }
}
