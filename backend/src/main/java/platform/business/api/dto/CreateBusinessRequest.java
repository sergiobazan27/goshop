package platform.business.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBusinessRequest(
  @NotBlank
  @Size(max = 150)
  String name,
  @NotBlank
  @Size(max = 255)
  String ownerKeycloakUserId
) {

}
