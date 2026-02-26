package com.atlas.user.service;


import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class OptionService {

    private final UserService userService;

    private final RoleService roleService;

    private final AuthorityService authorityService;

    private final DictionaryItemService dictionaryItemService;

    public List<OptionVO<Long>> userOptions() {
        List<UserVO> userList = userService.listUserOptions();
        if(CollectionUtils.isEmpty(userList)){
            return Collections.emptyList();
        }
        return userList.stream()
                .map(m -> OptionVO.of(m.getFullName(),m.getId()))
                .collect(Collectors.toList());
    }

    public List<OptionVO<Long>> roleOptions() {
        List<RoleVO> roleList = roleService.listRoleOptions();
        if(CollectionUtils.isEmpty(roleList)){
            return Collections.emptyList();
        }
        return roleList.stream()
                .map(m -> OptionVO.of(m.getName(),m.getId()))
                .collect(Collectors.toList());
    }

    public List<OptionVO<Long>> authorityTreeOption(){
        List<AuthorityVO> treeList = authorityService.tree();
        if(CollectionUtils.isEmpty(treeList)){
            return Collections.emptyList();
        }
        return OptionVO.copyTree(
                treeList,
                AuthorityVO::getName,
                AuthorityVO::getId,
                AuthorityVO::getChildren
        );
    }


    public List<DictionaryItemVO> dictOptions(String code,String category){
        List<DictionaryItemVO> dictionaryItemList = dictionaryItemService.findDictByCode(code);
        if(StringUtils.isNotEmpty(category)){
            String[] categoryArr = category.split(",");
            List<String> categoryList = Arrays.asList(categoryArr);
            dictionaryItemList = dictionaryItemList
                    .stream()
                    .filter(f -> categoryList.contains(f.getCategory()))
                    .collect(Collectors.toList());
        }
        return dictionaryItemList;
    }

    public List<DictionaryItemVO> dictTreeOptions(String code,String category){
        List<DictionaryItemVO> dictionaryItemList = this.dictOptions(code,category);
        return TreeUtils.buildTree(
                dictionaryItemList,
                DictionaryItemVO::getId,
                DictionaryItemVO::getParentId,
                DictionaryItemVO::setChildren,
                0L
        );

    }

}
