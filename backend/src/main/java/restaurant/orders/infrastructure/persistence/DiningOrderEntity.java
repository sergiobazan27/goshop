package restaurant.orders.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import restaurant.orders.domain.OrderStatus;
import restaurant.tables.infrastructure.persistence.DiningTableEntity;
import shared.TimestampedEntity;

@Entity
@Table(name = "restaurant_order", indexes = @Index(name = "idx_order_business", columnList = "business_id"))
public class DiningOrderEntity extends TimestampedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @Column(name = "business_id", nullable = false, updatable = false)
  public UUID businessId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "table_id", nullable = false, updatable = false)
  public DiningTableEntity table;

  // Closing releases the unique slot while preserving table_id for history.
  @Column(name = "open_table_id", unique = true)
  public UUID openTableId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public OrderStatus status = OrderStatus.OPEN;

  @Column(nullable = false)
  public long revision;

  @Column(name = "opened_by", nullable = false, updatable = false, length = 255)
  public String openedBy;

  @Column(name = "opened_by_subject", nullable = false, updatable = false, length = 255)
  public String openedBySubject;

  @Column(name = "closed_at")
  public Instant closedAt;

  @Column(name = "closed_by", length = 255)
  public String closedBy;

  @Column(name = "closed_by_subject", length = 255)
  public String closedBySubject;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt, id")
  @BatchSize(size = 100)
  public List<OrderItemEntity> items = new ArrayList<>();
}
