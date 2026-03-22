package com.atlas.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShortcutUpdateDTO {

    @NotNull
    @Size(max = 8, message = "快捷方式最多设置8个")
    private List<String> shortcuts;

}
