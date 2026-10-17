package platform.business.application;

import identity.infrastructure.keycloak.KeycloakUserDirectory;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import platform.business.infrastructure.persistence.BusinessEntity;

@ApplicationScoped 
public class BusinessOnboardingService {

    private final KeycloakUserDirectory userDirectory;
    private final BusinessService businessService;

    @Inject 
    public BusinessOnboardingService(
        KeycloakUserDirectory userDirectory,
        BusinessService businessService
    ) {
        this.userDirectory = userDirectory;
        this.businessService = businessService;
    }

    @RolesAllowed("admin")
    @Transactional(TxType.NOT_SUPPORTED)
    public BusinessEntity register(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String ownerKeycloakUserId
    ) {
        userDirectory.requireEnabledOwner(ownerKeycloakUserId);

        return businessService.createWithOwner(name, ownerKeycloakUserId);
    }
    
}
