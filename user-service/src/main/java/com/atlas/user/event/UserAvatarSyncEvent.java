package com.atlas.user.event;

import java.io.Serializable;

public record UserAvatarSyncEvent(Long userId, String avatarUrl) implements Serializable {
}
