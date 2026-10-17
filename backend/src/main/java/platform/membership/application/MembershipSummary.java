package platform.membership.application;

import java.util.UUID;

import platform.business.domain.BusinessStatus;
import platform.membership.domain.MembershipRole;

public record MembershipSummary(
  MembershipRole role,
  BusinessSummary business
) {
  public record BusinessSummary(
    UUID id,
    String name,
    BusinessStatus status
  ) {}
}
