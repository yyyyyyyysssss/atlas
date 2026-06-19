package com.atlas.common.core.api.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class UserIdentifierQueryDTO {

    private Collection<String> values;

    private String type;

    public static UserIdentifierQueryDTO ofEmail(List<String> values){
        UserIdentifierQueryDTO dto = new UserIdentifierQueryDTO();
        dto.values = values;
        dto.type = "email";
        return dto;
    }

    public static UserIdentifierQueryDTO ofPhone(List<String> values){
        UserIdentifierQueryDTO dto = new UserIdentifierQueryDTO();
        dto.values = values;
        dto.type = "phone";
        return dto;
    }

}
