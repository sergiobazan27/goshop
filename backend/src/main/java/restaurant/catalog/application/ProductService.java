package restaurant.catalog.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.HashMap;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import platform.business.application.CurrentBusinessService;
import platform.subscription.application.SubscriptionService;
import restaurant.catalog.infrastructure.persistence.ProductEntity;
import restaurant.catalog.infrastructure.persistence.ProductRepository;
import shared.application.BusinessConflictException;
import shared.application.BusinessRuleException;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
@RolesAllowed("owner")
public class ProductService {
  private final ProductRepository repository;
  private final CurrentBusinessService businesses;
  private final SubscriptionService subscriptions;

  @Inject
  public ProductService(ProductRepository repository, CurrentBusinessService businesses,
    SubscriptionService subscriptions) {
    this.repository = repository;
    this.businesses = businesses;
    this.subscriptions = subscriptions;
  }

  @Transactional
  @RolesAllowed({"owner", "waiter"})
  public Page<ProductSummary> list(@Size(max = 100) String search, Boolean available, int page, int size) {
    var business = businesses.requireActiveBusiness();
    var parameters = new HashMap<String, Object>();
    parameters.put("businessId", business.id());
    var filter = "businessId = :businessId";
    if (search != null && !search.isBlank()) {
      parameters.put("search", search.strip().toLowerCase(Locale.ROOT));
      filter += " and (locate(:search, normalizedName) > 0 or locate(:search, lower(description)) > 0)";
    }
    if (available != null) {
      parameters.put("available", available);
      filter += " and available = :available";
    }
    var query = repository.find(filter + " order by normalizedName, id", parameters);
    long total = query.count();
    var items = query.page(page, size).list().stream().map(this::summary).toList();
    return new Page<>(items, page, size, total, (total + size - 1) / size);
  }

  @Transactional
  @RolesAllowed({"owner", "waiter"})
  public long countAvailable() {
    var business = businesses.requireActiveBusiness();
    return repository.count("businessId = ?1 and available = true", business.id());
  }

  @Transactional
  @RolesAllowed({"owner", "waiter"})
  public ProductSummary requireAvailable(@NotNull UUID id) {
    var business = businesses.requireActiveBusiness();
    var product = repository.find("id = ?1 and businessId = ?2", id, business.id()).firstResultOptional()
      .orElseThrow(() -> new ResourceNotFoundException("Product.NotFound", "No se encontró el producto."));
    if (!product.available) {
      throw new BusinessRuleException("Product.Unavailable", "El producto no está disponible.");
    }
    return summary(product);
  }

  @Transactional
  public ProductSummary create(@NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal price,
    boolean available) {
    var business = businesses.lockActiveBusiness();
    var subscription = subscriptions.requireCurrent(business.id());
    if (repository.count("businessId", business.id()) >= subscription.plan().maxProducts()) {
      throw new BusinessRuleException("Product.LimitReached", "Has alcanzado el límite de productos de tu plan.");
    }
    var product = new ProductEntity();
    product.businessId = business.id();
    setDetails(product, name, description, price, available);
    repository.persist(product);
    return summary(product);
  }

  @Transactional
  public ProductSummary update(@NotNull UUID id, @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal price,
    boolean available) {
    var business = businesses.lockActiveBusiness();
    var product = repository.find("id = ?1 and businessId = ?2", id, business.id()).firstResultOptional()
      .orElseThrow(() -> new ResourceNotFoundException("Product.NotFound", "No se encontró el producto."));
    setDetails(product, name, description, price, available);
    return summary(product);
  }

  private void setDetails(ProductEntity product, String name, String description, BigDecimal price,
    boolean available) {
    var normalized = name.strip().toLowerCase(Locale.ROOT);
    boolean duplicate = product.id == null
      ? repository.count("businessId = ?1 and normalizedName = ?2", product.businessId, normalized) > 0
      : repository.count("businessId = ?1 and normalizedName = ?2 and id <> ?3",
          product.businessId, normalized, product.id) > 0;
    if (duplicate) {
      throw new BusinessConflictException("Product.DuplicateName", "Ya existe un producto con ese nombre.");
    }
    product.name = name.strip();
    product.normalizedName = normalized;
    product.description = description == null ? "" : description.strip();
    product.price = price.setScale(2, RoundingMode.UNNECESSARY);
    product.available = available;
  }

  private ProductSummary summary(ProductEntity product) {
    return new ProductSummary(product.id, product.name, product.description, product.price, product.available);
  }
}
