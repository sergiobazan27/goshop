package identity.infrastructure.keycloak;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import shared.application.BusinessRuleException;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
public class KeycloakUserDirectory {
  private final Keycloak keycloak;
  private final String realm;

  @Inject
  public KeycloakUserDirectory(
    Keycloak keycloak,
    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm") String realm
  ) {
    this.keycloak = keycloak;
    this.realm = realm;
  }

  @RolesAllowed("admin")
  @Transactional(TxType.NOT_SUPPORTED)
  public void requireEnabledOwner(@NotBlank @Size(max = 255) String userId) {
    try {
      // Authenticate the technical account before interpreting a user-resource 404.
      keycloak.tokenManager().getAccessTokenString();
      UserResource user = keycloak.realm(realm).users().get(userId);
      UserRepresentation representation;
      try {
        representation = user.toRepresentation();
      } catch (NotFoundException exception) {
        throw new ResourceNotFoundException(
          "Identity.UserNotFound", "No se encontró el usuario en Keycloak.");
      }

      if (!Boolean.TRUE.equals(representation.isEnabled())) {
        throw new BusinessRuleException(
          "Identity.UserDisabled", "El usuario está deshabilitado.");
      }

      boolean isOwner = user.roles().realmLevel().listEffective().stream()
        .anyMatch(role -> "owner".equals(role.getName()));
      if (!isOwner) {
        throw new BusinessRuleException(
          "Identity.OwnerRoleRequired", "El usuario no tiene el rol owner.");
      }
    } catch (WebApplicationException | ProcessingException exception) {
      // Technical-account failures must not appear as permission failures of the caller.
      throw new IdentityProviderException(exception);
    }
  }
}
