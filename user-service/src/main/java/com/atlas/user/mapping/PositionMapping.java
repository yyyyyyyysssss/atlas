package com.atlas.user.mapping;
import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;
import com.atlas.user.domain.dto.PositionCreateDTO;
import com.atlas.user.domain.dto.PositionUpdateDTO;
import com.atlas.user.domain.entity.Position;
import com.atlas.user.domain.vo.PositionVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true), uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface PositionMapping{
    
    PositionMapping INSTANCE = Mappers.getMapper(PositionMapping.class);
    
    PositionVO toPositionVO(Position entity);
    
    @IterableMapping(elementTargetType = PositionVO.class)
    List<PositionVO> toPositionVO(List<Position> list);
    
    Position toPosition(PositionCreateDTO createDTO);
    
    Position toPosition(PositionUpdateDTO updateDTO);
    
    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePosition(PositionUpdateDTO updateDTO, @MappingTarget Position entity);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwritePosition(PositionUpdateDTO updateDTO, @MappingTarget Position entity);
}

