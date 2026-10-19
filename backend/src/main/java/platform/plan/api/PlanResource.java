package platform.plan.api;

import java.util.UUID;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import platform.plan.api.dto.PlanRequest;
import platform.plan.api.dto.PlanResponse;
import platform.plan.application.PlanService;
import shared.application.Page;

@Path("/api/admin/plans")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlanResource {
  private final PlanService service;

  @Inject
  public PlanResource(PlanService service) {
    this.service = service;
  }

  @GET
  public Page<PlanResponse> list(
    @QueryParam("search") @DefaultValue("") @Size(max = 100) String search,
    @QueryParam("active") @Pattern(regexp = "true|false") String active,
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size
  ) {
    var result = service.list(search, active == null ? null : Boolean.valueOf(active), page, size);
    return new Page<>(result.items().stream().map(PlanResponse::from).toList(),
      result.page(), result.size(), result.total(), result.totalPages());
  }

  @POST
  public Response create(@NotNull @Valid PlanRequest request) {
    var plan = service.create(request.name(), request.maxTables().intValueExact(),
      request.maxUsers().intValueExact(), request.maxProducts().intValueExact(), request.active());
    return Response.status(Response.Status.CREATED).entity(PlanResponse.from(plan)).build();
  }

  @PUT
  @Path("/{id}")
  public PlanResponse update(@PathParam("id") UUID id, @NotNull @Valid PlanRequest request) {
    return PlanResponse.from(service.update(id, request.name(), request.maxTables().intValueExact(),
      request.maxUsers().intValueExact(), request.maxProducts().intValueExact(), request.active()));
  }
}
