package identity.infrastructure.keycloak;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class KeycloakUserDirectory {

    private final Keycloak keycloak;
    private final String realm;

    @Inject 
    public KeycloakUserDirectory(Keycloak keycloak, @ConfigProperty(name = "quarkus.keycloak.admin-client.realm") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @RolesAllowed("admin")
    @Transactional(TxType.NOT_SUPPORTED)
    public void requiredEnableOwner(@NotBlank @Size(max = 255) String userId){
       try {
        UserResource user = keycloak.realm(realm).users().get(userId);

        if (!Boolean.TRUE.equals(user.toRepresentation().isEnabled())) {
            throw new IllegalStateException("El usuario de Keycloak esta deshabilitado.");
        }

        boolean isOwner = user.roles()
            .realmLevel()
            .listEffective()
            .stream()
            .anyMatch(role -> "owner".equals(role.getName()));

        if (!isOwner) {
            throw new IllegalStateException("El usuario no tiene rol owner "); 
        }
       } catch (NotFoundException e) {
        throw new IllegalStateException("No se pudo verificar el usuario owner de Keycloak.", e);
       } 
    }

}
