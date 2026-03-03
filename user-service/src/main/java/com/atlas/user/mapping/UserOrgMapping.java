package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.entity.UserOrg;
import com.atlas.user.domain.dto.UserOrgDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface UserOrgMapping{
    
    UserOrgMapping INSTANCE = Mappers.getMapper(UserOrgMapping.class);
    
    @IterableMapping(elementTargetType = UserOrg.class)
    List<UserOrg> toUserOrg(List<UserOrgDTO> list);
    
    UserOrg toUserOrg(UserOrgDTO dto);
    
    UserOrgDTO toUserOrgDTO(UserOrg entity);
    
    @IterableMapping(elementTargetType = UserOrgDTO.class)
    List<UserOrgDTO> toUserOrgDTO(List<UserOrg> list);
}

