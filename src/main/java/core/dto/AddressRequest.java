package core.dto;

import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotNull(message = "street обязательное поле") String street,
        Long townId) {
}
