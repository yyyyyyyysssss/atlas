package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.vo.OrganizationVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface OrganizationMapping{
    
    OrganizationMapping INSTANCE = Mappers.getMapper(OrganizationMapping.class);
    
    OrganizationVO toOrganizationVO(Organization entity);
    
    @IterableMapping(elementTargetType = OrganizationVO.class)
    List<OrganizationVO> toOrganizationVO(List<Organization> list);
    
    Organization toOrganization(OrganizationCreateDTO createDTO);
    
    Organization toOrganization(OrganizationUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOrganization(OrganizationUpdateDTO updateDTO, @MappingTarget Organization entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteOrganization(OrganizationUpdateDTO updateDTO, @MappingTarget Organization entity);
}

