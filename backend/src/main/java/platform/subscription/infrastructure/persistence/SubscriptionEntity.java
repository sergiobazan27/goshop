package platform.subscription.infrastructure.persistence;

import java.util.UUID;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import platform.business.infrastructure.persistence.BusinessEntity;
import shared.TimestampedEntity;

@Entity
@Table(name = "subscription")
public class SubscriptionEntity extends TimestampedEntity {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "business_id", nullable = false, unique = true, updatable = false)
  public BusinessEntity business;
  @Embedded
  public SubscriptionTerms terms;
}
