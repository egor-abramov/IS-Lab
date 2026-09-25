package core.dto;

import java.util.List;

public record ItemsDto<T>(List<T> items) {
}
