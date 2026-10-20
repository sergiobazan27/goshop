package restaurant.orders.api;

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
import restaurant.orders.api.dto.OpenOrderRequest;
import restaurant.orders.api.dto.OrderItemRequest;
import restaurant.orders.api.dto.CloseOrderRequest;
import restaurant.orders.application.OrderHistoryEntry;
import restaurant.orders.application.OrderService;
import restaurant.orders.application.OrderSummary;
import restaurant.orders.application.TableOrderSummary;
import shared.application.Page;

@Path("/api/orders")
@RolesAllowed({"owner", "waiter"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
  private final OrderService service;

  @Inject
  public OrderResource(OrderService service) {
    this.service = service;
  }

  @GET
  @Path("/tables")
  public Page<TableOrderSummary> tables(
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("12") @Min(1) @Max(100) int size) {
    return service.tables(page, size);
  }

  @GET
  @Path("/history")
  public Page<OrderHistoryEntry> history(
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    return service.history(page, size);
  }

  @PUT
  @Path("/{id}/close")
  public OrderSummary close(@PathParam("id") UUID id, @NotNull @Valid CloseOrderRequest request) {
    return service.close(id, request.revision().longValueExact());
  }

  @POST
  public Response open(@NotNull @Valid OpenOrderRequest request) {
    return Response.status(Response.Status.CREATED).entity(service.open(request.tableId())).build();
  }

  @GET
  @Path("/{id}")
  public OrderSummary get(@PathParam("id") UUID id) {
    return service.get(id);
  }

  @PUT
  @Path("/{id}/items/{productId}")
  public OrderSummary setQuantity(@PathParam("id") UUID id, @PathParam("productId") UUID productId,
    @NotNull @Valid OrderItemRequest request) {
    return service.setQuantity(id, productId, request.quantity().intValueExact(), request.revision().longValueExact());
  }
}
