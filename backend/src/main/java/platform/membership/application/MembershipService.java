package platform.membership.application;

import java.util.Optional;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.membership.domain.MembershipRole;
import platform.membership.infrastructure.persistence.MembershipEntity;
import platform.membership.infrastructure.persistence.MembershipRepository;
import shared.application.BusinessConflictException;

@ApplicationScoped
public class MembershipService {

  private final MembershipRepository membershipRepository;

  @Inject
  public MembershipService(MembershipRepository membershipRepository) {
    this.membershipRepository = membershipRepository;
  }

  @Authenticated
  @Transactional
  public Optional<MembershipSummary> findSummaryByKeycloakUserId(
    @NotBlank @Size(max = 255) String keycloakUserId
  ) {
    return membershipRepository.findByKeycloakUserId(keycloakUserId)
      .map(membership -> {
        var business = membership.business;

        return new MembershipSummary(
          membership.role,
          new MembershipSummary.BusinessSummary(
            business.id,
            business.name,
            business.status
          )
        );
      });
  }

  @Transactional(TxType.MANDATORY)
  @RolesAllowed("admin")
  public MembershipEntity assignOwner(
    @NotNull BusinessEntity business,
    @NotBlank @Size(max = 255) String keycloakUserId
  ) {
    if (business.id == null) {
      throw new IllegalArgumentException(
        "El negocio debe estar persistido antes de asignar el dueño."
      );
    }

    membershipRepository.lockUserAssignment(keycloakUserId);

    if (membershipRepository
      .findByKeycloakUserId(keycloakUserId)
      .isPresent()
    ) {
      throw new BusinessConflictException(
        "Membership.UserAlreadyAssigned", "El usuario ya pertenece a un negocio.");
    }

    if (membershipRepository.existsOwnerByBusinessId(business.id)) {
      throw new BusinessConflictException(
        "Membership.OwnerAlreadyAssigned", "El negocio ya tiene un dueño.");
    }

    MembershipEntity membership = new MembershipEntity();
    membership.keycloakUserId = keycloakUserId;
    membership.business = business;
    membership.role = MembershipRole.OWNER;

    membershipRepository.persist(membership);

    return membership;
  }
}
