package platform.subscription.application;

import java.time.Instant;
import java.util.UUID;
import platform.subscription.domain.SubscriptionAction;

public record SubscriptionHistoryEntry(
  UUID id, SubscriptionAction action, String actorSubject, String actorName,
  Instant changedAt, SubscriptionSummary subscription
) {}
