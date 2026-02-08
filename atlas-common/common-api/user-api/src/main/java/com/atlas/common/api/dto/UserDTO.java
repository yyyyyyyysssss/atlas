package com.atlas.common.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

    private Long id;

    private String username;

    private String fullName;

    private String email;

    private String phone;

}
