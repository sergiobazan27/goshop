package platform.subscription.infrastructure.persistence;

import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionHistoryRepository implements PanacheRepositoryBase<SubscriptionHistoryEntity, UUID> {}
