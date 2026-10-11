package core.dto;

import java.util.List;

public record ErrorDto(List<String> errors) {
    public ErrorDto {
        errors = errors.stream()
                .map(ErrorDto::capitalize)
                .toList();
    }

    private static String capitalize(String e) {
        if (e.isEmpty() || Character.UnicodeScript.of(e.charAt(0)) != Character.UnicodeScript.CYRILLIC) {
            return e;
        }
        return e.substring(0, 1).toUpperCase() + e.substring(1);
    }
}
