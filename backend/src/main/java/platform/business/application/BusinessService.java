package platform.business.application;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.business.infrastructure.persistence.BusinessRepository;
import platform.membership.application.MembershipService;

@ApplicationScoped
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final MembershipService membershipService;

    @Inject
    public BusinessService(BusinessRepository businessRepository, MembershipService membershipService) {
        this.businessRepository = businessRepository;
        this.membershipService = membershipService;
    }

    @Transactional
    @RolesAllowed("admin")
    public BusinessEntity createWithOwner(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 255) String ownerKeycloakUserId) {
        BusinessEntity business = new BusinessEntity();
        business.name = name.strip();

        businessRepository.persist(business);

        membershipService.assignOwner(business, ownerKeycloakUserId);

        return business;
    }
}
