package platform.business.application;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import platform.business.domain.BusinessStatus;
import platform.membership.application.MembershipService;
import platform.membership.application.MembershipSummary.BusinessSummary;
import shared.application.AccessDeniedException;

@ApplicationScoped
public class CurrentBusinessService {
  private final JsonWebToken token;
  private final MembershipService membershipService;

  @Inject
  public CurrentBusinessService(JsonWebToken token, MembershipService membershipService) {
    this.token = token;
    this.membershipService = membershipService;
  }

  @RolesAllowed({"owner", "waiter"})
  public BusinessSummary requireActiveBusiness() {
    var membership = membershipService.findSummaryByKeycloakUserId(token.getSubject())
      .orElseThrow(() -> new AccessDeniedException(
        "Membership.Required", "Debes pertenecer a un negocio para realizar esta operación."));

    var business = membership.business();
    if (business.status() != BusinessStatus.ACTIVE) {
      throw new AccessDeniedException(
        "Business.Inactive", "El negocio no tiene habilitado el acceso operativo.");
    }

    return business;
  }
}
