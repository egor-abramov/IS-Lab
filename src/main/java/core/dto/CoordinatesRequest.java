package core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CoordinatesRequest(
        @NotNull(message = "x обязательное поле")
        @DecimalMin(value = "-577", inclusive = false, message = "x должно быть больше -577") Float x,
        @NotNull(message = "y обязательное поле") Integer y) {
}
