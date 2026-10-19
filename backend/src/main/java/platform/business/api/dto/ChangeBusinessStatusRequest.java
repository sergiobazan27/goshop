package platform.business.api.dto;

import jakarta.validation.constraints.NotNull;
import platform.business.domain.BusinessStatus;

public record ChangeBusinessStatusRequest(@NotNull BusinessStatus status) {}
