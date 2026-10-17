package platform.membership.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import platform.membership.domain.MembershipRole;

@ApplicationScoped 
public class MembershipRepository implements PanacheRepositoryBase<MembershipEntity, UUID> {

    public Optional<MembershipEntity> findByKeycloakUserId(String userId) {
        return find("keycloakUserId", userId).firstResultOptional();
    }

    public boolean existsOwnerByBusinessId(UUID businessId) {
        return count(
            "business.id = ?1 and role = ?2",
            businessId,
            MembershipRole.OWNER
        ) > 0;
    }
}
