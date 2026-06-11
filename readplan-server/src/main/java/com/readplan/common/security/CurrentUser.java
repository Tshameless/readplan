package com.readplan.common.security;

import java.util.List;

public record CurrentUser(
    Long id,
    String username,
    String nickname,
    List<String> roles,
    List<String> permissions
) {

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }
}
