package platform.business.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import platform.business.domain.BusinessStatus;
import shared.TimestampedEntity;

@Entity
@Table(name = "business")
public class BusinessEntity extends TimestampedEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    public String name;

    @NotNull 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public BusinessStatus status = BusinessStatus.ACTIVE; 
}
