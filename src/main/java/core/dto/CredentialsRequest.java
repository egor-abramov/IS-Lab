package core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CredentialsRequest(
        @NotBlank(message = "логин обязателен")
        @Pattern(regexp = "[a-zA-Z0-9_]{3,64}", message = "логин: от 3 до 64 латинских букв, цифр или подчёркиваний")
        String login,
        @NotBlank(message = "пароль обязателен")
        @Size(min = 8, max = 128, message = "пароль должен содержать от 8 до 128 символов")
        String password) {
}
