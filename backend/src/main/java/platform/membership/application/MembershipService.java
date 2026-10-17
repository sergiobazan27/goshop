package platform.membership.application;

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

@ApplicationScoped
public class MembershipService {

    private final MembershipRepository membershipRepository;

    @Inject 
    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
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
        
        if (membershipRepository
            .findByKeycloakUserId(keycloakUserId)
            .isPresent()
        ) {
            throw new IllegalArgumentException("El usuario ya pertenece a un negocio");
        }

        if (membershipRepository.existsOwnerByBusinessId(business.id)) {
            throw new IllegalArgumentException("El negocio ya tiene dueño.");
        }

        MembershipEntity membership = new MembershipEntity();
        membership.keycloakUserId = keycloakUserId;
        membership.business = business;
        membership.role = MembershipRole.OWNER;

        membershipRepository.persist(membership);

        return membership;
    }
}
