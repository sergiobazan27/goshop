package platform.membership.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTeamMemberRequest(@NotBlank @Size(max = 255) String keycloakUserId) {}
