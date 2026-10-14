package platform.membership.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.membership.domain.MembershipRole;
import shared.TimestampedEntity;

@Entity
@Table(
    name = "membership",
    indexes = @Index(
        name = "idx_membership_business",
        columnList = "business_id"
    )
)
public class MembershipEntity extends TimestampedEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(
        name = "keycloak_user_id",
        nullable = false,
        unique = true,
        length = 255,
        updatable = false
    )
    public String keycloakUserId;

    @NotNull 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    public BusinessEntity business;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public MembershipRole role;
}
