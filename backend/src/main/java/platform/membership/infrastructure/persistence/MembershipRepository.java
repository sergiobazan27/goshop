package platform.membership.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import platform.membership.domain.MembershipRole;

@ApplicationScoped 
public class MembershipRepository implements PanacheRepositoryBase<MembershipEntity, UUID> {

    public Set<String> assignedUserIds() {
        return new HashSet<>(getEntityManager()
            .createQuery("select m.keycloakUserId from MembershipEntity m", String.class).getResultList());
    }

    public void lockUserAssignment(String userId) {
        // Serialize assignments of the same user even when different businesses are involved.
        getEntityManager().unwrap(org.hibernate.Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                statement.setString(1, "membership:" + userId);
                statement.execute();
            }
        });
    }

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
