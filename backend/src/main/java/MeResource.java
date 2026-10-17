import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import platform.business.application.CurrentBusinessService;
import platform.membership.application.MembershipService;
import platform.membership.application.MembershipSummary.BusinessSummary;
import platform.membership.domain.MembershipRole;

@Path("/api/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {
  @Inject
  JsonWebToken token;

  @Inject
  SecurityIdentity identity;

  @Inject
  MembershipService membershipService;

  @Inject
  CurrentBusinessService currentBusinessService;

  @GET
  public MeResponse me() {
    var membership = membershipService
      .findSummaryByKeycloakUserId(token.getSubject())
      .orElse(null);

    return new MeResponse(
      token.getSubject(),
      identity.getPrincipal().getName(),
      identity.getRoles(),
      membership == null ? null : membership.role(),
      membership == null ? null : membership.business()
    );
  }

  @GET
  @Path("/owner-check")
  @RolesAllowed("owner")
  public MeResponse ownerCheck() {
    currentBusinessService.requireActiveBusiness();
    return me();
  }

  public record MeResponse(
    String sub,
    String username,
    Set<String> roles,
    MembershipRole membershipRole,
    BusinessSummary business
  ) {}
}
