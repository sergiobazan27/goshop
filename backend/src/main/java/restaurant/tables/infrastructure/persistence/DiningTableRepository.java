package restaurant.tables.infrastructure.persistence;

import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DiningTableRepository implements PanacheRepositoryBase<DiningTableEntity, UUID> {}
