package platform.plan.application;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import platform.plan.infrastructure.persistence.PlanEntity;
import platform.plan.infrastructure.persistence.PlanRepository;
import shared.application.Page;
import shared.application.ResourceNotFoundException;
import shared.application.BusinessRuleException;

@ApplicationScoped
@RolesAllowed("admin")
public class PlanService {
  private final PlanRepository repository;

  @Inject
  public PlanService(PlanRepository repository) {
    this.repository = repository;
  }

  @Transactional(TxType.MANDATORY)
  public PlanEntity requireAvailable(@NotNull UUID id) {
    var plan = repository.findById(id, LockModeType.PESSIMISTIC_READ);
    if (plan == null) throw new ResourceNotFoundException("Plan.NotFound", "No se encontró el plan.");
    if (!plan.active) throw new BusinessRuleException("Plan.Inactive", "Elige un plan activo.");
    return plan;
  }

  @Transactional
  public Page<PlanEntity> list(String search, Boolean active, int page, int size) {
    var query = repository.search(search, active);
    long total = query.count();
    return new Page<>(query.page(page, size).list(), page, size, total, (total + size - 1) / size);
  }

  @Transactional
  public PlanEntity create(
    @NotBlank @Size(max = 100) String name,
    @Positive int maxTables, @Positive int maxUsers, @Positive int maxProducts, boolean active
  ) {
    var plan = new PlanEntity();
    setDetails(plan, name, maxTables, maxUsers, maxProducts, active);
    repository.persist(plan);
    return plan;
  }

  @Transactional
  public PlanEntity update(
    @NotNull UUID id, @NotBlank @Size(max = 100) String name,
    @Positive int maxTables, @Positive int maxUsers, @Positive int maxProducts, boolean active
  ) {
    var plan = repository.findByIdOptional(id).orElseThrow(() ->
      new ResourceNotFoundException("Plan.NotFound", "No se encontró el plan."));
    setDetails(plan, name, maxTables, maxUsers, maxProducts, active);
    return plan;
  }

  private void setDetails(PlanEntity plan, String name, int tables, int users, int products, boolean active) {
    plan.name = name.strip();
    plan.maxTables = tables;
    plan.maxUsers = users;
    plan.maxProducts = products;
    plan.active = active;
  }
}
