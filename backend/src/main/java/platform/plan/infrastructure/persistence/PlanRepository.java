package platform.plan.infrastructure.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlanRepository implements PanacheRepositoryBase<PlanEntity, UUID> {
  public PanacheQuery<PlanEntity> search(String search, Boolean active) {
    var conditions = new ArrayList<String>();
    var parameters = new HashMap<String, Object>();
    if (search != null && !search.isBlank()) {
      var name = search.strip().toLowerCase(Locale.ROOT)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
      conditions.add("lower(name) like :name escape '!'");
      parameters.put("name", "%" + name + "%");
    }
    if (active != null) {
      conditions.add("active = :active");
      parameters.put("active", active);
    }
    var sort = Sort.by("createdAt", Sort.Direction.Descending).and("id", Sort.Direction.Descending);
    return conditions.isEmpty() ? findAll(sort) : find(String.join(" and ", conditions), sort, parameters);
  }
}
