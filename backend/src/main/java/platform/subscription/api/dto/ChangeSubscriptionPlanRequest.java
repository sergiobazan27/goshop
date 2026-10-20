package platform.subscription.api.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record ChangeSubscriptionPlanRequest(@NotNull UUID planId) {}
