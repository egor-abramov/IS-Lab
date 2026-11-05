package core.dto;

import core.model.AppUser;
import core.model.Role;

public record UserDto(Long id, String login, Role role) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getLogin(), user.getRole());
    }
}
