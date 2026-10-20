package identity.infrastructure.keycloak;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
import identity.application.DirectoryUser;

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

  @RolesAllowed("owner")
  @Transactional(TxType.NOT_SUPPORTED)
  public DirectoryUser requireEnabledWaiter(@NotBlank @Size(max = 255) String userId) {
    try {
      keycloak.tokenManager().getAccessTokenString();
      var user = keycloak.realm(realm).users().get(userId);
      UserRepresentation representation;
      try {
        representation = user.toRepresentation();
      } catch (NotFoundException exception) {
        throw new ResourceNotFoundException("Identity.UserNotFound", "No se encontró la cuenta en Keycloak.");
      }
      if (!Boolean.TRUE.equals(representation.isEnabled())) {
        throw new BusinessRuleException("Identity.UserDisabled", "La cuenta está deshabilitada.");
      }
      var roles = user.roles().realmLevel().listEffective();
      if (roles.stream().noneMatch(role -> "waiter".equals(role.getName()))) {
        throw new BusinessRuleException("Identity.WaiterRoleRequired", "La cuenta necesita el rol waiter.");
      }
      if (roles.stream().anyMatch(role -> "owner".equals(role.getName()) || "admin".equals(role.getName()))) {
        throw new BusinessRuleException("Identity.PrivilegedUser", "No puedes vincular una cuenta owner o admin como mozo.");
      }
      return directoryUser(representation);
    } catch (WebApplicationException | ProcessingException exception) {
      throw new IdentityProviderException(exception);
    }
  }

  @RolesAllowed("owner")
  @Transactional(TxType.NOT_SUPPORTED)
  public List<DirectoryUser> availableWaiters(@Size(max = 255) String search, Set<String> assignedIds) {
    try {
      keycloak.tokenManager().getAccessTokenString();
      var users = keycloak.realm(realm).users();
      var available = new ArrayList<DirectoryUser>();
      int batchSize = 100;
      // ponytail: scan matching enabled accounts for exact eligible totals; index the directory if the realm outgrows this flow.
      for (int offset = 0; ; offset += batchSize) {
        var batch = users.search(search == null || search.isBlank() ? null : search.strip(), true, offset, batchSize);
        for (var candidate : batch) {
          if (!Boolean.TRUE.equals(candidate.isEnabled()) || assignedIds.contains(candidate.getId())) continue;
          try {
            var roles = users.get(candidate.getId()).roles().realmLevel().listEffective();
            boolean waiter = roles.stream().anyMatch(role -> "waiter".equals(role.getName()));
            boolean privileged = roles.stream().anyMatch(role -> "owner".equals(role.getName()) || "admin".equals(role.getName()));
            if (waiter && !privileged) available.add(directoryUser(candidate));
          } catch (NotFoundException exception) {
            // A user deleted during enumeration is no longer selectable.
          }
        }
        if (batch.size() < batchSize) break;
      }
      available.sort(Comparator.comparing(DirectoryUser::username, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(DirectoryUser::id));
      return available;
    } catch (WebApplicationException | ProcessingException exception) {
      throw new IdentityProviderException(exception);
    }
  }

  private DirectoryUser directoryUser(UserRepresentation user) {
    var name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
      + (user.getLastName() == null ? "" : user.getLastName())).strip();
    return new DirectoryUser(user.getId(), user.getUsername(), name.isEmpty() ? user.getUsername() : name);
  }
}
