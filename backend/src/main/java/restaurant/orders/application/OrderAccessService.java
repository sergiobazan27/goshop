package restaurant.orders.application;

import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import restaurant.orders.infrastructure.persistence.OrderRepository;
import shared.application.BusinessConflictException;

@ApplicationScoped
public class OrderAccessService {
  private final OrderRepository repository;

  @Inject
  public OrderAccessService(OrderRepository repository) {
    this.repository = repository;
  }

  @Transactional(TxType.MANDATORY)
  @RolesAllowed("owner")
  public void requireTableUnused(UUID businessId, UUID tableId) {
    if (repository.count("businessId = ?1 and table.id = ?2", businessId, tableId) > 0) {
      throw new BusinessConflictException("Table.HasOrders", "No puedes eliminar una mesa que tiene pedidos asociados.");
    }
  }
}
