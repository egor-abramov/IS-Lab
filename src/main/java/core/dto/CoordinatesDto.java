package core.dto;

import core.model.Coordinates;

public record CoordinatesDto(Long id, float x, int y) {
    public static CoordinatesDto from(Coordinates c) {
        return c == null ? null : new CoordinatesDto(c.getId(), c.getX(), c.getY());
    }
}
