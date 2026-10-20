package platform.subscription.api.dto;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record RenewSubscriptionRequest(
  @NotNull UUID planId, Instant startsAt, @NotNull Instant expiresAt
) {}
