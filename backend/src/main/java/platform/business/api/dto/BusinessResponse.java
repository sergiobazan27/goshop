package platform.business.api.dto;

import java.time.Instant;
import java.util.UUID;

import platform.business.domain.BusinessStatus;
import platform.business.infrastructure.persistence.BusinessEntity;

public record BusinessResponse(
    UUID id,
    String name,
    BusinessStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static BusinessResponse from(BusinessEntity business) {
        return new BusinessResponse(
            business.id, 
            business.name, 
            business.status, 
            business.createdAt, 
            business.updatedAt);
    }
}
