package platform.membership.application;

import java.util.UUID;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;
import identity.application.DirectoryUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import platform.business.application.CurrentBusinessService;
import platform.membership.application.MembershipSummary.BusinessSummary;
import platform.membership.domain.MembershipRole;
import platform.membership.infrastructure.persistence.MembershipEntity;
import platform.membership.infrastructure.persistence.MembershipRepository;
import platform.subscription.application.SubscriptionService;
import shared.application.AccessDeniedException;
import shared.application.BusinessConflictException;
import shared.application.BusinessRuleException;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
@RolesAllowed("owner")
public class TeamService {
  private final MembershipRepository repository;
  private final CurrentBusinessService businesses;
  private final SubscriptionService subscriptions;
  private final JsonWebToken token;

  @Inject
  public TeamService(MembershipRepository repository, CurrentBusinessService businesses,
    SubscriptionService subscriptions, JsonWebToken token) {
    this.repository = repository;
    this.businesses = businesses;
    this.subscriptions = subscriptions;
    this.token = token;
  }

  @Transactional
  public BusinessSummary requireOwner() {
    return requireOwnerBusiness(false);
  }

  @Transactional
  public Page<TeamMemberSummary> list(int page, int size) {
    var business = requireOwnerBusiness(false);
    var query = repository.find("business.id = ?1 order by role, createdAt, id", business.id());
    long total = query.count();
    var items = query.page(page, size).list().stream().map(this::summary).toList();
    return new Page<>(items, page, size, total, (total + size - 1) / size);
  }

  @Transactional
  public Set<String> assignedUserIds() {
    requireOwnerBusiness(false);
    return repository.assignedUserIds();
  }

  @Transactional
  public TeamMemberSummary add(@NotNull DirectoryUser user) {
    var business = requireOwnerBusiness(true);
    repository.lockUserAssignment(user.id());
    if (repository.findByKeycloakUserId(user.id()).isPresent()) {
      throw new BusinessConflictException("Membership.UserAlreadyAssigned", "La cuenta ya pertenece a un negocio.");
    }
    var subscription = subscriptions.requireCurrent(business.id());
    if (repository.count("business.id", business.id()) >= subscription.plan().maxUsers()) {
      throw new BusinessRuleException("Membership.LimitReached", "Has alcanzado el límite de usuarios de tu plan, incluido el dueño.");
    }
    var member = new MembershipEntity();
    member.business = repository.getEntityManager().getReference(platform.business.infrastructure.persistence.BusinessEntity.class, business.id());
    member.keycloakUserId = user.id();
    member.username = user.username();
    member.role = MembershipRole.WAITER;
    repository.persist(member);
    return summary(member);
  }

  @Transactional
  public void remove(@NotNull UUID id) {
    var business = requireOwnerBusiness(true);
    var member = repository.find("id = ?1 and business.id = ?2", id, business.id()).firstResultOptional()
      .orElseThrow(() -> new ResourceNotFoundException("Membership.NotFound", "No se encontró el integrante."));
    if (member.role == MembershipRole.OWNER) {
      throw new BusinessRuleException("Membership.OwnerProtected", "No puedes desvincular al dueño del negocio.");
    }
    repository.delete(member);
  }

  private BusinessSummary requireOwnerBusiness(boolean lock) {
    var business = lock ? businesses.lockActiveBusiness() : businesses.requireActiveBusiness();
    var member = repository.findByKeycloakUserId(token.getSubject())
      .orElseThrow(() -> new AccessDeniedException("Membership.Required", "Debes pertenecer a un negocio."));
    if (member.role != MembershipRole.OWNER || !member.business.id.equals(business.id())) {
      throw new AccessDeniedException("Membership.OwnerRequired", "Solo el dueño puede gestionar el equipo.");
    }
    return business;
  }

  private TeamMemberSummary summary(MembershipEntity member) {
    return new TeamMemberSummary(member.id, member.keycloakUserId, member.username, member.role, member.createdAt);
  }
}
