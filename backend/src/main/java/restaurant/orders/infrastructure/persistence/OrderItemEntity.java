package restaurant.orders.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import shared.TimestampedEntity;

@Entity
@Table(name = "order_item", uniqueConstraints = @UniqueConstraint(
  name = "uq_order_item_product", columnNames = {"order_id", "product_id"}))
public class OrderItemEntity extends TimestampedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, updatable = false)
  public DiningOrderEntity order;

  @Column(name = "product_id", nullable = false, updatable = false)
  public UUID productId;

  @Column(name = "product_name", nullable = false, updatable = false, length = 100)
  public String productName;

  @Column(name = "unit_price", nullable = false, updatable = false, precision = 12, scale = 2)
  public BigDecimal unitPrice;

  @Column(nullable = false)
  public int quantity;
}
