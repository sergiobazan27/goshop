package restaurant.orders.application;

import java.util.List;

public record OrderOverview(long openOrders, List<OrderActivitySummary> recentOrders,
  List<OrderActivitySummary> activity) {}
