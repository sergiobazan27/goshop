package restaurant.orders.infrastructure.persistence;

import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import restaurant.tables.infrastructure.persistence.DiningTableEntity;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<DiningOrderEntity, UUID> {
  public DiningTableEntity tableReference(UUID id) {
    return getEntityManager().getReference(DiningTableEntity.class, id);
  }
}
