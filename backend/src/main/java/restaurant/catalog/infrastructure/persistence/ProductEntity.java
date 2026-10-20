package restaurant.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import shared.TimestampedEntity;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(
  name = "uq_product_business_name", columnNames = {"business_id", "normalized_name"}))
public class ProductEntity extends TimestampedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @Column(name = "business_id", nullable = false, updatable = false)
  public UUID businessId;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(name = "normalized_name", nullable = false, length = 200)
  public String normalizedName;

  @Column(nullable = false, length = 1000)
  public String description;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal price;

  @Column(nullable = false)
  public boolean available;
}
