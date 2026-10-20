package restaurant.overview.application;

import java.time.Instant;
import java.util.List;
import restaurant.orders.application.OrderActivitySummary;
import restaurant.orders.application.TableOrderSummary;

public record OverviewSummary(Instant generatedAt, Metrics metrics, List<TableOrderSummary> tables,
  List<OrderActivitySummary> recentOrders, List<OrderActivitySummary> activity) {
  public record Metrics(long totalTables, long occupiedTables, long openOrders, long availableProducts) {}
}
