package core.dto;

import jakarta.validation.constraints.NotNull;

public record CoordinatesRequest(
        @NotNull(message = "x is required") Float x,
        @NotNull(message = "y is required") Integer y) {
}
