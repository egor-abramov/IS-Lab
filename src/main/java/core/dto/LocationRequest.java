package core.dto;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull(message = "x is required") Long x,
        Float y,
        String name) {
}
