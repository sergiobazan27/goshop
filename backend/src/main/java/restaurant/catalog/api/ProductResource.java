package restaurant.catalog.api;

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
import restaurant.catalog.api.dto.ProductRequest;
import restaurant.catalog.application.ProductService;
import restaurant.catalog.application.ProductSummary;
import shared.application.Page;

@Path("/api/products")
@RolesAllowed("owner")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {
  private final ProductService service;

  @Inject
  public ProductResource(ProductService service) {
    this.service = service;
  }

  @GET
  @RolesAllowed({"owner", "waiter"})
  public Page<ProductSummary> list(
    @QueryParam("search") @DefaultValue("") @Size(max = 100) String search,
    @QueryParam("available") @Pattern(regexp = "true|false") String available,
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    return service.list(search, available == null ? null : Boolean.valueOf(available), page, size);
  }

  @POST
  public Response create(@NotNull @Valid ProductRequest request) {
    var product = service.create(request.name(), request.description(), request.price(), request.available());
    return Response.status(Response.Status.CREATED).entity(product).build();
  }

  @PUT
  @Path("/{id}")
  public ProductSummary update(@PathParam("id") UUID id, @NotNull @Valid ProductRequest request) {
    return service.update(id, request.name(), request.description(), request.price(), request.available());
  }
}
