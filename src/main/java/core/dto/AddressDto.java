package core.dto;

import core.model.Address;

public record AddressDto(Long id, String street, LocationDto town, Long ownerId) {
    public static AddressDto from(Address a) {
        return a == null ? null : new AddressDto(a.getId(), a.getStreet(), LocationDto.from(a.getTown()), a.getOwnerId());
    }
}
