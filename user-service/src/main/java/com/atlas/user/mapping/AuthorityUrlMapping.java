package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.dto.AuthorityUrlDTO;
import com.atlas.user.domain.entity.AuthorityUrl;
import org.mapstruct.Builder;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface AuthorityUrlMapping{
    
    AuthorityUrlMapping INSTANCE = Mappers.getMapper(AuthorityUrlMapping.class);
    
    @IterableMapping(elementTargetType = AuthorityUrl.class)
    List<AuthorityUrl> toAuthorityUrl(List<AuthorityUrlDTO> list);
    
    AuthorityUrl toAuthorityUrl(AuthorityUrlDTO dto);
    
    AuthorityUrlDTO toAuthorityUrlDTO(AuthorityUrl entity);
    
    @IterableMapping(elementTargetType = AuthorityUrlDTO.class)
    List<AuthorityUrlDTO> toAuthorityUrlDTO(List<AuthorityUrl> list);
}

