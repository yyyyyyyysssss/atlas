package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.entity.RoleDataScope;
import com.atlas.user.domain.dto.RoleDataScopeDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface RoleDataScopeMapping{
    
    RoleDataScopeMapping INSTANCE = Mappers.getMapper(RoleDataScopeMapping.class);
    
    @IterableMapping(elementTargetType = RoleDataScope.class)
    List<RoleDataScope> toRoleDataScope(List<RoleDataScopeDTO> list);
    
    RoleDataScope toRoleDataScope(RoleDataScopeDTO dto);
    
    RoleDataScopeDTO toRoleDataScopeDTO(RoleDataScope entity);
    
    @IterableMapping(elementTargetType = RoleDataScopeDTO.class)
    List<RoleDataScopeDTO> toRoleDataScopeDTO(List<RoleDataScope> list);
}

