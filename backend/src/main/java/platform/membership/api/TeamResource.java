package platform.membership.api;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import platform.membership.api.dto.AddTeamMemberRequest;
import platform.membership.application.TeamMemberSummary;
import platform.membership.application.TeamOnboardingService;
import platform.membership.application.TeamService;
import shared.application.Page;
import identity.application.DirectoryUser;

@Path("/api/team")
@RolesAllowed("owner")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TeamResource {
  private final TeamService team;
  private final TeamOnboardingService onboarding;

  @Inject
  public TeamResource(TeamService team, TeamOnboardingService onboarding) {
    this.team = team;
    this.onboarding = onboarding;
  }

  @GET
  public Page<TeamMemberSummary> list(
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    return team.list(page, size);
  }

  @POST
  public Response add(@NotNull @Valid AddTeamMemberRequest request) {
    return Response.status(Response.Status.CREATED).entity(onboarding.add(request.keycloakUserId())).build();
  }

  @GET
  @Path("/available-waiters")
  public Page<DirectoryUser> availableWaiters(
    @QueryParam("search") @DefaultValue("") @Size(max = 255) String search,
    @QueryParam("page") @DefaultValue("0") @Min(0) @Max(1_000_000) int page,
    @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size) {
    return onboarding.availableWaiters(search, page, size);
  }

  @DELETE
  @Path("/{id}")
  public Response remove(@PathParam("id") UUID id) {
    team.remove(id);
    return Response.noContent().build();
  }
}
