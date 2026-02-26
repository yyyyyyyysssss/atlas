package com.atlas.user.service;

import com.atlas.user.domain.dto.DictionaryCreateDTO;
import com.atlas.user.domain.dto.DictionaryQueryDTO;
import com.atlas.user.domain.dto.DictionaryUpdateDTO;
import com.atlas.user.domain.entity.Dictionary;
import com.atlas.user.domain.vo.DictionaryVO;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


public interface DictionaryService extends IService<Dictionary> {

    Long createDictionary(DictionaryCreateDTO createDTO);

    void updateDictionary(DictionaryUpdateDTO updateDTO, Boolean isFullUpdate);

    PageInfo<DictionaryVO> queryList(DictionaryQueryDTO queryDTO);

    DictionaryVO details(Long id);

    DictionaryVO findByCode(String code);

    void deleteDictionary(Long id);

}
