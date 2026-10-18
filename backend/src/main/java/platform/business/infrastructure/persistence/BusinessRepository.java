package platform.business.infrastructure.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import platform.business.domain.BusinessStatus;

@ApplicationScoped
public class BusinessRepository implements PanacheRepositoryBase<BusinessEntity, UUID> {
  public PanacheQuery<BusinessEntity> search(String search, BusinessStatus status) {
    var conditions = new ArrayList<String>();
    var parameters = new HashMap<String, Object>();
    if (search != null && !search.isBlank()) {
      // Treat LIKE wildcards as literal characters in a business name.
      var name = search.strip().toLowerCase(Locale.ROOT)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
      conditions.add("lower(name) like :name escape '!'");
      parameters.put("name", "%" + name + "%");
    }
    if (status != null) {
      conditions.add("status = :status");
      parameters.put("status", status);
    }
    var sort = Sort.by("createdAt", Sort.Direction.Descending)
      .and("id", Sort.Direction.Descending);
    return conditions.isEmpty()
      ? findAll(sort)
      : find(String.join(" and ", conditions), sort, parameters);
  }
}
