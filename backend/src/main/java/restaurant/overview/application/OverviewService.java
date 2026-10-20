package restaurant.overview.application;

import java.time.Instant;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import restaurant.catalog.application.ProductService;
import restaurant.orders.application.OrderService;

@ApplicationScoped
@RolesAllowed({"owner", "waiter"})
public class OverviewService {
  private final OrderService orders;
  private final ProductService products;

  @Inject
  public OverviewService(OrderService orders, ProductService products) {
    this.orders = orders;
    this.products = products;
  }

  @Transactional
  public OverviewSummary get() {
    var tables = orders.tables(0, 12);
    var orderOverview = orders.overview();
    long availableProducts = products.countAvailable();
    var metrics = new OverviewSummary.Metrics(tables.total(), orderOverview.openOrders(),
      orderOverview.openOrders(), availableProducts);
    return new OverviewSummary(Instant.now(), metrics, tables.items(), orderOverview.recentOrders(), orderOverview.activity());
  }
}
