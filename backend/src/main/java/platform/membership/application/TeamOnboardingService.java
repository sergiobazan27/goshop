package platform.membership.application;

import identity.infrastructure.keycloak.KeycloakUserDirectory;
import identity.application.DirectoryUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import shared.application.Page;

@ApplicationScoped
@RolesAllowed("owner")
public class TeamOnboardingService {
  private final TeamService team;
  private final KeycloakUserDirectory directory;

  @Inject
  public TeamOnboardingService(TeamService team, KeycloakUserDirectory directory) {
    this.team = team;
    this.directory = directory;
  }

  @Transactional(TxType.NOT_SUPPORTED)
  public TeamMemberSummary add(@NotBlank @Size(max = 255) String keycloakUserId) {
    team.requireOwner();
    var user = directory.requireEnabledWaiter(keycloakUserId);
    return team.add(user);
  }

  @Transactional(TxType.NOT_SUPPORTED)
  public Page<DirectoryUser> availableWaiters(String search, int page, int size) {
    var assigned = team.assignedUserIds();
    var users = directory.availableWaiters(search, assigned);
    int from = (int) Math.min((long) page * size, users.size());
    int to = (int) Math.min((long) from + size, users.size());
    return new Page<>(users.subList(from, to), page, size, users.size(), (users.size() + (long) size - 1) / size);
  }
}
