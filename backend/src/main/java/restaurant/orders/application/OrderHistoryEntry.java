package restaurant.orders.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderHistoryEntry(UUID id, String tableName, Instant closedAt, String closedBy, BigDecimal total) {}
