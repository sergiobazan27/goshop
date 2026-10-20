package platform.subscription.infrastructure.persistence;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import platform.plan.infrastructure.persistence.PlanEntity;

@Embeddable
public class SubscriptionTerms {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id", nullable = false)
  public PlanEntity plan;
  @Column(name = "plan_name", nullable = false, length = 100)
  public String planName;
  @Column(name = "max_tables", nullable = false)
  public int maxTables;
  @Column(name = "max_users", nullable = false)
  public int maxUsers;
  @Column(name = "max_products", nullable = false)
  public int maxProducts;
  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;
  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  public SubscriptionTerms copy() {
    var copy = new SubscriptionTerms();
    copy.plan = plan; copy.planName = planName;
    copy.maxTables = maxTables; copy.maxUsers = maxUsers; copy.maxProducts = maxProducts;
    copy.startsAt = startsAt; copy.expiresAt = expiresAt;
    return copy;
  }
}
