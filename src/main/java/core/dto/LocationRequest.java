package core.dto;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull(message = "x обязательное поле") Long x,
        @NotNull(message = "y обязательное поле") Float y,
        @NotNull(message = "name обязательное поле") String name) {
}
