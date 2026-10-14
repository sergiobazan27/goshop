package platform.membership.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class MembershipRepository implements PanacheRepositoryBase<MembershipEntity, UUID> {

    public Optional<MembershipEntity> findByKeycloakUserId(String userId) {
        return find("keycloakUserId", userId).firstResultOptional();
    }
}
