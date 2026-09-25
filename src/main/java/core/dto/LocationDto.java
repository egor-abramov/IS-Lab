package core.dto;

import core.model.Location;

public record LocationDto(Long id, long x, Float y, String name) {
    public static LocationDto from(Location l) {
        return l == null ? null : new LocationDto(l.getId(), l.getX(), l.getY(), l.getName());
    }
}
