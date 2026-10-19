package platform.business.application;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import platform.business.domain.BusinessStatus;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.business.infrastructure.persistence.BusinessRepository;
import platform.membership.application.MembershipService;
import shared.application.Page;
import shared.application.ResourceNotFoundException;

@ApplicationScoped
public class BusinessService {

  private final BusinessRepository businessRepository;
  private final MembershipService membershipService;

  @Inject
  public BusinessService(
    BusinessRepository businessRepository,
    MembershipService membershipService
  ) {
    this.businessRepository = businessRepository;
    this.membershipService = membershipService;
  }

  @Transactional
  @RolesAllowed("admin")
  public BusinessEntity changeStatus(@NotNull UUID id, @NotNull BusinessStatus status) {
    var business = businessRepository.findByIdOptional(id)
      .orElseThrow(() -> new ResourceNotFoundException(
        "Business.NotFound", "No se encontró el negocio."));
    if (business.status != status) {
      business.status = status;
    }
    return business;
  }

  @Transactional
  @RolesAllowed("admin")
  public Page<BusinessEntity> list(String search, BusinessStatus status, int page, int size) {
    var query = businessRepository.search(search, status);
    long total = query.count();
    var items = query.page(page, size).list();
    return new Page<>(items, page, size, total, (total + size - 1) / size);
  }

  @Transactional
  @RolesAllowed("admin")
  public BusinessEntity createWithOwner(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 255) String ownerKeycloakUserId
  ) {
    BusinessEntity business = new BusinessEntity();
    business.name = name.strip();

    businessRepository.persist(business);

    membershipService.assignOwner(business, ownerKeycloakUserId);

    return business;
  }
}
