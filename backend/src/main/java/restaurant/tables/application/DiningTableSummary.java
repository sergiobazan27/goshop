package restaurant.tables.application;

import java.util.UUID;

public record DiningTableSummary(UUID id, String name, int capacity) {}
