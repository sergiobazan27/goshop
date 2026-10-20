package platform.subscription.application;

import java.time.Instant;
import java.util.UUID;
import platform.subscription.domain.SubscriptionStatus;

public record SubscriptionSummary(
  UUID id, UUID businessId, PlanSnapshot plan, Instant startsAt, Instant expiresAt,
  SubscriptionStatus status
) {
  public record PlanSnapshot(UUID id, String name, int maxTables, int maxUsers, int maxProducts) {}
}
