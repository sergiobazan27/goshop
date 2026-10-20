package platform.business.application;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import platform.business.domain.BusinessStatus;
import platform.membership.application.MembershipService;
import platform.membership.application.MembershipSummary.BusinessSummary;
import shared.application.AccessDeniedException;
import platform.subscription.application.SubscriptionService;

@ApplicationScoped
public class CurrentBusinessService {
  private final JsonWebToken token;
  private final MembershipService membershipService;
  private final SubscriptionService subscriptions;
  private final BusinessService businesses;

  @Inject
  public CurrentBusinessService(JsonWebToken token, MembershipService membershipService,
    SubscriptionService subscriptions, BusinessService businesses) {
    this.token = token;
    this.membershipService = membershipService;
    this.subscriptions = subscriptions;
    this.businesses = businesses;
  }

  @RolesAllowed({"owner", "waiter"})
  public BusinessSummary requireActiveBusiness() {
    return requireBusiness(false);
  }

  @RolesAllowed({"owner", "waiter"})
  @Transactional(TxType.MANDATORY)
  public BusinessSummary lockActiveBusiness() {
    // ponytail: serialize business writes; narrow locks if restaurant throughput requires it.
    return requireBusiness(true);
  }

  private BusinessSummary requireBusiness(boolean lock) {
    var membership = membershipService.findSummaryByKeycloakUserId(token.getSubject())
      .orElseThrow(() -> new AccessDeniedException(
        "Membership.Required", "Debes pertenecer a un negocio para realizar esta operación."));

    var business = lock ? businesses.lockForOperation(membership.business().id()) : membership.business();
    if (lock) {
      var current = membershipService.findSummaryByKeycloakUserId(token.getSubject())
        .orElseThrow(() -> new AccessDeniedException("Membership.Required", "Debes pertenecer a un negocio para realizar esta operación."));
      if (!current.business().id().equals(business.id())) {
        throw new AccessDeniedException("Membership.Changed", "Tu membresía cambió. Vuelve a iniciar sesión.");
      }
    }
    if (business.status() != BusinessStatus.ACTIVE) {
      throw new AccessDeniedException(
        "Business.Inactive", "El negocio no tiene habilitado el acceso operativo.");
    }

    subscriptions.requireCurrent(business.id());
    return business;
  }
}
