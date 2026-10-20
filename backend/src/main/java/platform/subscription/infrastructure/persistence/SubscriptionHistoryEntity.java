package platform.subscription.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.Table;
import platform.subscription.domain.SubscriptionAction;

@Entity
@Immutable
@Table(name = "subscription_history", indexes = @Index(name = "idx_subscription_history", columnList = "subscription_id,changed_at"))
public class SubscriptionHistoryEntity {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subscription_id", nullable = false, updatable = false)
  public SubscriptionEntity subscription;
  @Embedded
  public SubscriptionTerms terms;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  public SubscriptionAction action;
  @Column(name = "actor_subject", nullable = false, length = 255, updatable = false)
  public String actorSubject;
  @Column(name = "actor_name", nullable = false, length = 255, updatable = false)
  public String actorName;
  @Column(name = "changed_at", nullable = false, updatable = false)
  public Instant changedAt;
}
