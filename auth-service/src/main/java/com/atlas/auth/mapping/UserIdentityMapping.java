package com.atlas.auth.mapping;

import com.atlas.auth.domain.dto.UserIdentityDTO;
import com.atlas.auth.domain.entity.UserIdentity;
import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface UserIdentityMapping {
    
    UserIdentityMapping INSTANCE = Mappers.getMapper(UserIdentityMapping.class);
    
    @IterableMapping(elementTargetType = UserIdentity.class)
    List<UserIdentity> toUserIdentity(List<UserIdentityDTO> list);
    
    UserIdentity toUserIdentity(UserIdentityDTO dto);
    
    UserIdentityDTO toUserIdentityDTO(UserIdentity entity);
    
    @IterableMapping(elementTargetType = UserIdentityDTO.class)
    List<UserIdentityDTO> toUserIdentityDTO(List<UserIdentity> list);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserIdentity(UserIdentityDTO userIdentityDTO, @MappingTarget UserIdentity userIdentity);
}

