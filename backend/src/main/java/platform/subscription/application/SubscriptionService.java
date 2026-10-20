package platform.subscription.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import platform.business.application.BusinessService;
import platform.plan.application.PlanService;
import platform.subscription.domain.SubscriptionAction;
import platform.subscription.domain.SubscriptionStatus;
import platform.subscription.infrastructure.persistence.SubscriptionEntity;
import platform.subscription.infrastructure.persistence.SubscriptionHistoryEntity;
import platform.subscription.infrastructure.persistence.SubscriptionHistoryRepository;
import platform.subscription.infrastructure.persistence.SubscriptionRepository;
import platform.subscription.infrastructure.persistence.SubscriptionTerms;
import shared.application.AccessDeniedException;
import shared.application.BusinessConflictException;
import shared.application.BusinessRuleException;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
public class SubscriptionService {
  private final SubscriptionRepository repository;
  private final SubscriptionHistoryRepository history;
  private final BusinessService businesses;
  private final PlanService plans;
  private final JsonWebToken token;
  private final SecurityIdentity identity;

  @Inject
  public SubscriptionService(SubscriptionRepository repository, SubscriptionHistoryRepository history,
    BusinessService businesses, PlanService plans, JsonWebToken token, SecurityIdentity identity) {
    this.repository = repository; this.history = history; this.businesses = businesses;
    this.plans = plans; this.token = token; this.identity = identity;
  }

  @Authenticated
  @Transactional
  public Optional<SubscriptionSummary> findByBusinessId(@NotNull UUID businessId) {
    var now = Instant.now();
    return repository.findByBusinessId(businessId).map(s -> summary(s, s.terms, now));
  }

  @RolesAllowed({"owner", "waiter"})
  @Transactional
  public SubscriptionSummary requireCurrent(@NotNull UUID businessId) {
    var subscription = findByBusinessId(businessId).orElseThrow(() ->
      new AccessDeniedException("Subscription.Required", "El negocio necesita una suscripción."));
    if (subscription.status() == SubscriptionStatus.NOT_STARTED) {
      throw new AccessDeniedException("Subscription.NotStarted", "La suscripción todavía no ha comenzado.");
    }
    if (subscription.status() == SubscriptionStatus.EXPIRED) {
      throw new AccessDeniedException("Subscription.Expired", "La suscripción del negocio ha vencido.");
    }
    return subscription;
  }

  @Transactional
  @RolesAllowed("admin")
  public SubscriptionSummary assign(@NotNull UUID businessId, @NotNull UUID planId,
    @NotNull Instant startsAt, @NotNull Instant expiresAt) {
    var business = businesses.lockForSubscription(businessId);
    if (repository.findByBusinessId(businessId).isPresent()) {
      throw new BusinessConflictException("Subscription.AlreadyAssigned", "El negocio ya tiene una suscripción. Usa renovar o cambiar plan.");
    }
    var now = Instant.now();
    validateDates(startsAt, expiresAt, now);
    var subscription = new SubscriptionEntity();
    subscription.business = business;
    subscription.terms = terms(planId, startsAt, expiresAt);
    repository.persist(subscription);
    record(subscription, SubscriptionAction.ASSIGNED, now);
    return summary(subscription, subscription.terms, now);
  }

  @Transactional
  @RolesAllowed("admin")
  public SubscriptionSummary renew(@NotNull UUID businessId, @NotNull UUID planId,
    Instant startsAt, @NotNull Instant expiresAt) {
    businesses.lockForSubscription(businessId);
    var subscription = requireSubscription(businessId);
    var now = Instant.now();
    var previous = subscription.terms;
    Instant start;
    if (now.isBefore(previous.expiresAt)) {
      start = previous.startsAt;
      if (startsAt != null && !startsAt.equals(start)) {
        throw new BusinessRuleException("Subscription.StartFixed", "Una suscripción sin vencer conserva su fecha de inicio.");
      }
      if (!expiresAt.isAfter(previous.expiresAt)) {
        throw new BusinessRuleException("Subscription.ExtensionRequired", "La renovación debe extender el vencimiento actual.");
      }
    } else {
      start = startsAt == null ? now : startsAt;
      if (start.isBefore(previous.expiresAt)) {
        throw new BusinessRuleException("Subscription.InvalidStart", "El nuevo inicio no puede ser anterior al vencimiento previo.");
      }
    }
    validateDates(start, expiresAt, now);
    subscription.terms = terms(planId, start, expiresAt);
    record(subscription, SubscriptionAction.RENEWED, now);
    return summary(subscription, subscription.terms, now);
  }

  @Transactional
  @RolesAllowed("admin")
  public SubscriptionSummary changePlan(@NotNull UUID businessId, @NotNull UUID planId) {
    businesses.lockForSubscription(businessId);
    var subscription = requireSubscription(businessId);
    var current = subscription.terms;
    var now = Instant.now();
    if (current.plan.id.equals(planId)) {
      return summary(subscription, current, now);
    }
    subscription.terms = terms(planId, current.startsAt, current.expiresAt);
    record(subscription, SubscriptionAction.PLAN_CHANGED, now);
    return summary(subscription, subscription.terms, now);
  }

  @Transactional
  @RolesAllowed("admin")
  public Page<SubscriptionHistoryEntry> history(@NotNull UUID businessId, int page, int size) {
    var query = history.find("subscription.business.id = ?1 order by changedAt desc, id desc", businessId);
    long total = query.count();
    var items = query.page(page, size).list().stream().map(entry ->
      new SubscriptionHistoryEntry(entry.id, entry.action, entry.actorSubject, entry.actorName,
        entry.changedAt, summary(entry.subscription, entry.terms, entry.changedAt))).toList();
    return new Page<>(items, page, size, total, (total + size - 1) / size);
  }

  private SubscriptionEntity requireSubscription(UUID businessId) {
    return repository.findByBusinessId(businessId).orElseThrow(() ->
      new ResourceNotFoundException("Subscription.NotFound", "No se encontró una suscripción para el negocio."));
  }

  private void validateDates(Instant start, Instant end, Instant now) {
    if (!end.isAfter(start) || !end.isAfter(now)) {
      throw new BusinessRuleException("Subscription.InvalidDates", "El vencimiento debe ser posterior al inicio y a la hora actual.");
    }
  }

  private SubscriptionTerms terms(UUID planId, Instant start, Instant end) {
    var plan = plans.requireAvailable(planId);
    var terms = new SubscriptionTerms();
    terms.plan = plan; terms.planName = plan.name;
    terms.maxTables = plan.maxTables; terms.maxUsers = plan.maxUsers; terms.maxProducts = plan.maxProducts;
    terms.startsAt = start; terms.expiresAt = end;
    return terms;
  }

  private void record(SubscriptionEntity subscription, SubscriptionAction action, Instant now) {
    var entry = new SubscriptionHistoryEntity();
    entry.subscription = subscription; entry.terms = subscription.terms.copy(); entry.action = action;
    entry.actorSubject = token.getSubject(); entry.actorName = identity.getPrincipal().getName();
    entry.changedAt = now;
    history.persist(entry);
  }

  private SubscriptionSummary summary(SubscriptionEntity subscription, SubscriptionTerms terms, Instant now) {
    var status = now.isBefore(terms.startsAt) ? SubscriptionStatus.NOT_STARTED
      : now.isBefore(terms.expiresAt) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED;
    return new SubscriptionSummary(subscription.id, subscription.business.id,
      new SubscriptionSummary.PlanSnapshot(terms.plan.id, terms.planName, terms.maxTables,
        terms.maxUsers, terms.maxProducts), terms.startsAt, terms.expiresAt, status);
  }
}
