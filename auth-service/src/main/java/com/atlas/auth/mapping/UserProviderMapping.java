package com.atlas.auth.mapping;

import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserProvider;
import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface UserProviderMapping {
    
    UserProviderMapping INSTANCE = Mappers.getMapper(UserProviderMapping.class);
    
    UserProviderDTO toUserProviderDTO(UserProvider entity);
}

