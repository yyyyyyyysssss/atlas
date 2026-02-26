package com.atlas.user.service;



import com.atlas.user.domain.dto.DictionaryItemCreateDTO;
import com.atlas.user.domain.dto.DictionaryItemQueryDTO;
import com.atlas.user.domain.dto.DictionaryItemUpdateDTO;
import com.atlas.user.domain.entity.DictionaryItem;
import com.atlas.user.domain.vo.DictionaryItemVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface DictionaryItemService extends IService<DictionaryItem> {

    Long createDictionaryItem(DictionaryItemCreateDTO createDTO);

    void updateDictionaryItem(DictionaryItemUpdateDTO updateDTO, Boolean isFullUpdate);

    void updateStatus(Long id, Boolean enabled);

    PageInfo<DictionaryItemVO> queryList(DictionaryItemQueryDTO queryDTO);

    DictionaryItemVO details(Long id);

    List<DictionaryItemVO> findChildrenById(Long id);

    List<DictionaryItemVO> findChildrenById(List<Long> ids);

    void deleteDictionaryItem(Long roleId);

    List<DictionaryItemVO> findDictByCode(String code);

}
