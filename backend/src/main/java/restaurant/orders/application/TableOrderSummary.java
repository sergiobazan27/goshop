package restaurant.orders.application;

import java.math.BigDecimal;
import java.util.UUID;
import restaurant.tables.application.DiningTableSummary;

public record TableOrderSummary(DiningTableSummary table, UUID orderId, BigDecimal total, long quantity) {}
