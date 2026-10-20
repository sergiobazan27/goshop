package restaurant.tables.api.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiningTableRequest(
  @NotBlank @Size(max = 100) String name,
  @NotNull @DecimalMin("1") @DecimalMax("2147483647") @Digits(integer = 10, fraction = 0) BigDecimal capacity
) {}
