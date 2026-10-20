package platform.membership.application;

import java.time.Instant;
import java.util.UUID;
import platform.membership.domain.MembershipRole;

public record TeamMemberSummary(UUID id, String keycloakUserId, String username,
  MembershipRole role, Instant joinedAt) {}
