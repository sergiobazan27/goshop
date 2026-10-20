package restaurant.tables.api;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import restaurant.tables.api.dto.DiningTableRequest;
import restaurant.tables.application.DiningTableService;
import restaurant.tables.application.DiningTableSummary;
import shared.application.Page;

@Path("/api/tables")
@RolesAllowed("owner")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DiningTableResource {
  private final DiningTableService service;

  @Inject
  public DiningTableResource(DiningTableService service) {
    this.service = service;
  }

  @GET
  public Page<DiningTableSummary> list(
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    return service.list(page, size);
  }

  @POST
  public Response create(@NotNull @Valid DiningTableRequest request) {
    var table = service.create(request.name(), request.capacity().intValueExact());
    return Response.status(Response.Status.CREATED).entity(table).build();
  }

  @PUT
  @Path("/{id}")
  public DiningTableSummary update(@PathParam("id") UUID id, @NotNull @Valid DiningTableRequest request) {
    return service.update(id, request.name(), request.capacity().intValueExact());
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    service.delete(id);
    return Response.noContent().build();
  }
}
