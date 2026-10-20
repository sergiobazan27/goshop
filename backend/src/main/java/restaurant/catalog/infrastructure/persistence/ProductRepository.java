package restaurant.catalog.infrastructure.persistence;

import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<ProductEntity, UUID> {}
