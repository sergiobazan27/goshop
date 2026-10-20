package platform.subscription.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepositoryBase<SubscriptionEntity, UUID> {
  public Optional<SubscriptionEntity> findByBusinessId(UUID id) {
    return find("business.id", id).firstResultOptional();
  }
}
