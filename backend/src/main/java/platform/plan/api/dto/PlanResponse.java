package platform.plan.api.dto;

import java.time.Instant;
import java.util.UUID;
import platform.plan.infrastructure.persistence.PlanEntity;

public record PlanResponse(
  UUID id, String name, int maxTables, int maxUsers, int maxProducts,
  boolean active, Instant createdAt, Instant updatedAt
) {
  public static PlanResponse from(PlanEntity plan) {
    return new PlanResponse(plan.id, plan.name, plan.maxTables, plan.maxUsers,
      plan.maxProducts, plan.active, plan.createdAt, plan.updatedAt);
  }
}
