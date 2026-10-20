package restaurant.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(UUID id, String name, String description, BigDecimal price, boolean available) {}
