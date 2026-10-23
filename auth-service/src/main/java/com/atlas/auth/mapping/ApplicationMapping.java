package com.atlas.auth.mapping;

import com.atlas.auth.domain.dto.ApplicationSaveDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.vo.ApplicationVO;
import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 9:34
 */
@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface ApplicationMapping {

    ApplicationMapping INSTANCE = Mappers.getMapper(ApplicationMapping.class);


    Application toApplication(ApplicationSaveDTO saveDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateApplication(ApplicationSaveDTO saveDTO, @MappingTarget Application application);

    ApplicationVO toApplicationVO(Application application);

}
