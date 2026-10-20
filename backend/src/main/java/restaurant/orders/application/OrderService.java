package restaurant.orders.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import platform.business.application.CurrentBusinessService;
import restaurant.catalog.application.ProductService;
import restaurant.orders.domain.OrderStatus;
import restaurant.orders.infrastructure.persistence.DiningOrderEntity;
import restaurant.orders.infrastructure.persistence.OrderItemEntity;
import restaurant.orders.infrastructure.persistence.OrderRepository;
import restaurant.tables.application.DiningTableService;
import restaurant.tables.application.DiningTableSummary;
import shared.application.BusinessConflictException;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
@RolesAllowed({"owner", "waiter"})
public class OrderService {
  private final OrderRepository repository;
  private final CurrentBusinessService businesses;
  private final DiningTableService tables;
  private final ProductService products;
  private final JsonWebToken token;
  private final SecurityIdentity identity;

  @Inject
  public OrderService(OrderRepository repository, CurrentBusinessService businesses,
    DiningTableService tables, ProductService products, JsonWebToken token, SecurityIdentity identity) {
    this.repository = repository;
    this.businesses = businesses;
    this.tables = tables;
    this.products = products;
    this.token = token;
    this.identity = identity;
  }

  @Transactional
  public Page<TableOrderSummary> tables(int page, int size) {
    var business = businesses.requireActiveBusiness();
    var result = tables.list(page, size);
    var orders = new HashMap<UUID, DiningOrderEntity>();
    if (!result.items().isEmpty()) {
      var ids = result.items().stream().map(DiningTableSummary::id).toList();
      repository.find("select distinct o from DiningOrderEntity o left join fetch o.items "
        + "where o.businessId = ?1 and o.table.id in ?2 and o.status = ?3",
        business.id(), ids, OrderStatus.OPEN).list().forEach(order -> orders.put(order.table.id, order));
    }
    var items = result.items().stream().map(table -> {
      var order = orders.get(table.id());
      return new TableOrderSummary(table, order == null ? null : order.id,
        order == null ? BigDecimal.ZERO.setScale(2) : total(order),
        order == null ? 0 : order.items.stream().mapToLong(item -> item.quantity).sum());
    }).toList();
    return new Page<>(items, result.page(), result.size(), result.total(), result.totalPages());
  }

  @Transactional
  public OrderOverview overview() {
    var business = businesses.requireActiveBusiness();
    var open = repository.find("businessId = ?1 and status = ?2 order by createdAt desc, id desc",
      business.id(), OrderStatus.OPEN);
    long openCount = open.count();
    var recent = open.page(0, 5).list().stream().map(this::activitySummary).toList();
    var activity = repository.find("businessId = ?1 order by updatedAt desc, id desc", business.id())
      .page(0, 10).list().stream().map(this::activitySummary).toList();
    return new OrderOverview(openCount, recent, activity);
  }

  @Transactional
  public OrderSummary open(@NotNull UUID tableId) {
    var business = businesses.lockActiveBusiness();
    var table = tables.requireForOrder(tableId);
    if (repository.count("businessId = ?1 and openTableId = ?2", business.id(), tableId) > 0) {
      throw new BusinessConflictException("Order.TableOccupied", "La mesa ya tiene un pedido abierto. Actualiza las mesas para consultarlo.");
    }
    var order = new DiningOrderEntity();
    order.businessId = business.id();
    order.table = repository.tableReference(table.id());
    order.openTableId = table.id();
    order.openedBy = identity.getPrincipal().getName();
    order.openedBySubject = token.getSubject();
    repository.persist(order);
    return summary(order);
  }

  @Transactional
  public OrderSummary get(@NotNull UUID id) {
    var business = businesses.requireActiveBusiness();
    return summary(requireOrder(id, business.id()));
  }

  @Transactional
  public Page<OrderHistoryEntry> history(int page, int size) {
    var business = businesses.requireActiveBusiness();
    var query = repository.find("businessId = ?1 and status = ?2 order by closedAt desc, id desc",
      business.id(), OrderStatus.CLOSED);
    long count = query.count();
    var items = query.page(page, size).list().stream().map(order ->
      new OrderHistoryEntry(order.id, order.table.name, order.closedAt, order.closedBy, total(order))).toList();
    return new Page<>(items, page, size, count, (count + size - 1) / size);
  }

  @Transactional
  public OrderSummary close(@NotNull UUID id, @Min(0) long revision) {
    var business = businesses.lockActiveBusiness();
    var order = requireOrder(id, business.id());
    if (order.status == OrderStatus.CLOSED) return summary(order);
    requireRevision(order, revision);
    order.status = OrderStatus.CLOSED;
    order.openTableId = null;
    order.closedAt = Instant.now();
    order.closedBy = identity.getPrincipal().getName();
    order.closedBySubject = token.getSubject();
    order.revision++;
    order.updatedAt = order.closedAt;
    return summary(order);
  }

  @Transactional
  public OrderSummary setQuantity(@NotNull UUID id, @NotNull UUID productId,
    @Min(0) @Max(999) int quantity, @Min(0) long revision) {
    var business = businesses.lockActiveBusiness();
    var order = requireOrder(id, business.id());
    if (order.status != OrderStatus.OPEN) {
      throw new BusinessConflictException("Order.NotOpen", "Solo puedes modificar pedidos abiertos.");
    }
    requireRevision(order, revision);
    var item = order.items.stream().filter(line -> line.productId.equals(productId)).findFirst().orElse(null);
    if (item == null && quantity == 0) {
      throw new ResourceNotFoundException("Order.ItemNotFound", "El producto no está en este pedido.");
    }
    if (item != null && item.quantity == quantity) return summary(order);
    if (quantity == 0) {
      order.items.remove(item);
    } else {
      if (item == null || quantity > item.quantity) {
        var product = products.requireAvailable(productId);
        if (item == null) {
          item = new OrderItemEntity();
          item.order = order;
          item.productId = product.id();
          item.productName = product.name();
          item.unitPrice = product.price();
          order.items.add(item);
        }
      }
      item.quantity = quantity;
    }
    order.revision++;
    order.updatedAt = Instant.now();
    return summary(order);
  }

  private void requireRevision(DiningOrderEntity order, long revision) {
    if (order.revision != revision) {
      throw new BusinessConflictException("Order.Changed", "El pedido cambió. Actualízalo antes de volver a guardar.");
    }
  }

  private DiningOrderEntity requireOrder(UUID id, UUID businessId) {
    return repository.find("id = ?1 and businessId = ?2", id, businessId).firstResultOptional()
      .orElseThrow(() -> new ResourceNotFoundException("Order.NotFound", "No se encontró el pedido."));
  }

  private BigDecimal total(DiningOrderEntity order) {
    return order.items.stream().map(item -> item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)))
      .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
  }

  private OrderActivitySummary activitySummary(DiningOrderEntity order) {
    long quantity = order.items.stream().mapToLong(item -> item.quantity).sum();
    var products = order.items.stream().map(item -> item.quantity + " " + item.productName)
      .collect(Collectors.joining(", "));
    return new OrderActivitySummary(order.id, order.table.name, order.status, order.createdAt,
      order.updatedAt, quantity, products, total(order));
  }

  private OrderSummary summary(DiningOrderEntity order) {
    var items = order.items.stream().map(item -> new OrderSummary.Item(item.productId, item.productName,
      item.unitPrice, item.quantity, item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)))).toList();
    return new OrderSummary(order.id, new DiningTableSummary(order.table.id, order.table.name, order.table.capacity),
      order.status, order.revision, order.createdAt, order.openedBy, order.closedAt, order.closedBy, items, total(order));
  }
}
