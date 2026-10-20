package platform.subscription.api.dto;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record AssignSubscriptionRequest(
  @NotNull UUID planId, @NotNull Instant startsAt, @NotNull Instant expiresAt
) {}
