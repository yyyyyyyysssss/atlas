package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.dto.UserIdentityDTO;
import com.atlas.user.domain.entity.UserIdentity;
import org.mapstruct.Builder;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface UserIdentityMapping{
    
    UserIdentityMapping INSTANCE = Mappers.getMapper(UserIdentityMapping.class);
    
    @IterableMapping(elementTargetType = UserIdentity.class)
    List<UserIdentity> toUserIdentity(List<UserIdentityDTO> list);
    
    UserIdentity toUserIdentity(UserIdentityDTO dto);
    
    UserIdentityDTO toUserIdentityDTO(UserIdentity entity);
    
    @IterableMapping(elementTargetType = UserIdentityDTO.class)
    List<UserIdentityDTO> toUserIdentityDTO(List<UserIdentity> list);
}

