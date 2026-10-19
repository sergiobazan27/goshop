package platform.plan.infrastructure.persistence;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import shared.TimestampedEntity;

@Entity
@Table(name = "plan")
public class PlanEntity extends TimestampedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  @NotBlank
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  public String name;

  @Positive
  @Column(name = "max_tables", nullable = false)
  public int maxTables;

  @Positive
  @Column(name = "max_users", nullable = false)
  public int maxUsers;

  @Positive
  @Column(name = "max_products", nullable = false)
  public int maxProducts;

  @Column(nullable = false)
  public boolean active = true;
}
