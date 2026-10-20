package restaurant.tables.application;

import java.util.Locale;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.business.application.CurrentBusinessService;
import platform.subscription.application.SubscriptionService;
import restaurant.orders.application.OrderAccessService;
import restaurant.tables.infrastructure.persistence.DiningTableEntity;
import restaurant.tables.infrastructure.persistence.DiningTableRepository;
import shared.application.BusinessConflictException;
import shared.application.BusinessRuleException;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
@RolesAllowed("owner")
public class DiningTableService {
  private final DiningTableRepository repository;
  private final CurrentBusinessService businesses;
  private final SubscriptionService subscriptions;
  private final OrderAccessService orders;

  @Inject
  public DiningTableService(DiningTableRepository repository, CurrentBusinessService businesses,
    SubscriptionService subscriptions, OrderAccessService orders) {
    this.repository = repository;
    this.businesses = businesses;
    this.subscriptions = subscriptions;
    this.orders = orders;
  }

  @Transactional
  @RolesAllowed({"owner", "waiter"})
  public Page<DiningTableSummary> list(int page, int size) {
    var business = businesses.requireActiveBusiness();
    var query = repository.find("businessId = ?1 order by normalizedName, id", business.id());
    long total = query.count();
    var items = query.page(page, size).list().stream().map(this::summary).toList();
    return new Page<>(items, page, size, total, (total + size - 1) / size);
  }

  @Transactional(TxType.MANDATORY)
  @RolesAllowed({"owner", "waiter"})
  public DiningTableSummary requireForOrder(@NotNull UUID id) {
    var business = businesses.requireActiveBusiness();
    return summary(requireTable(id, business.id()));
  }

  @Transactional
  public DiningTableSummary create(@NotBlank @Size(max = 100) String name, @Positive int capacity) {
    var business = businesses.lockActiveBusiness();
    var subscription = subscriptions.requireCurrent(business.id());
    if (repository.count("businessId", business.id()) >= subscription.plan().maxTables()) {
      throw new BusinessRuleException("Table.LimitReached", "Has alcanzado el límite de mesas de tu plan.");
    }
    var table = new DiningTableEntity();
    table.businessId = business.id();
    setDetails(table, name, capacity);
    repository.persist(table);
    return summary(table);
  }

  @Transactional
  public DiningTableSummary update(@NotNull UUID id, @NotBlank @Size(max = 100) String name,
    @Positive int capacity) {
    var business = businesses.lockActiveBusiness();
    var table = requireTable(id, business.id());
    setDetails(table, name, capacity);
    return summary(table);
  }

  @Transactional
  public void delete(@NotNull UUID id) {
    var business = businesses.lockActiveBusiness();
    var table = requireTable(id, business.id());
    orders.requireTableUnused(business.id(), table.id);
    repository.delete(table);
  }

  private DiningTableEntity requireTable(UUID id, UUID businessId) {
    return repository.find("id = ?1 and businessId = ?2", id, businessId).firstResultOptional()
      .orElseThrow(() -> new ResourceNotFoundException("Table.NotFound", "No se encontró la mesa."));
  }

  private void setDetails(DiningTableEntity table, String name, int capacity) {
    var normalized = name.strip().toLowerCase(Locale.ROOT);
    boolean duplicate = table.id == null
      ? repository.count("businessId = ?1 and normalizedName = ?2", table.businessId, normalized) > 0
      : repository.count("businessId = ?1 and normalizedName = ?2 and id <> ?3",
          table.businessId, normalized, table.id) > 0;
    if (duplicate) {
      throw new BusinessConflictException("Table.DuplicateName", "Ya existe una mesa con ese nombre.");
    }
    table.name = name.strip();
    table.normalizedName = normalized;
    table.capacity = capacity;
  }

  private DiningTableSummary summary(DiningTableEntity table) {
    return new DiningTableSummary(table.id, table.name, table.capacity);
  }
}
