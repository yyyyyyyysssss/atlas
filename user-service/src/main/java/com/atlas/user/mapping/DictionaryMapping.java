package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;

import com.atlas.user.domain.dto.DictionaryCreateDTO;
import com.atlas.user.domain.dto.DictionaryItemCreateDTO;
import com.atlas.user.domain.dto.DictionaryItemUpdateDTO;
import com.atlas.user.domain.dto.DictionaryUpdateDTO;
import com.atlas.user.domain.entity.Dictionary;
import com.atlas.user.domain.entity.DictionaryItem;
import com.atlas.user.domain.vo.DictionaryItemVO;
import com.atlas.user.domain.vo.DictionaryVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true),uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface DictionaryMapping {

    DictionaryMapping INSTANCE = Mappers.getMapper(DictionaryMapping.class);

    Dictionary toDictionary(DictionaryCreateDTO dictionaryCreateDTO);

    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDictionary(DictionaryUpdateDTO dictionaryUpdateDTO, @MappingTarget Dictionary dictionary);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteDictionary(DictionaryUpdateDTO dictionaryUpdateDTO, @MappingTarget Dictionary dictionary);

    DictionaryVO toDictionaryVO(Dictionary dictionary);

    @IterableMapping(elementTargetType = DictionaryVO.class)
    List<DictionaryVO> toDictionaryVO(List<Dictionary> dictionaryList);


    DictionaryItem toDictionaryItem(DictionaryItemCreateDTO dictionaryItemCreateDTO);

    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDictionaryItem(DictionaryItemUpdateDTO dictionaryItemUpdateDTO, @MappingTarget DictionaryItem dictionaryItem);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteDictionaryItem(DictionaryItemUpdateDTO dictionaryItemUpdateDTO, @MappingTarget DictionaryItem dictionaryItem);

    DictionaryItemVO toDictionaryItemVO(DictionaryItem dictionaryItem);

    @IterableMapping(elementTargetType = DictionaryItemVO.class)
    List<DictionaryItemVO> toDictionaryItemVO(List<DictionaryItem> dictionaryItems);

}
