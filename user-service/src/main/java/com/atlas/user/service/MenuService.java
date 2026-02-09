package com.atlas.user.service;

import com.atlas.user.domain.dto.MenuCreateDTO;
import com.atlas.user.domain.dto.MenuDragDTO;
import com.atlas.user.domain.dto.MenuQueryDTO;
import com.atlas.user.domain.dto.MenuUpdateDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.vo.MenuVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


import java.util.Collection;
import java.util.List;

public interface MenuService extends IService<Authority> {

    Long createMenu(MenuCreateDTO menuCreateDTO);

    Integer updateMenu(MenuUpdateDTO menuUpdateDTO);

    Boolean deleteMenu(Long id);

    Boolean menuDrag(MenuDragDTO menuDragDTO);

    List<MenuVO> tree();

    PageInfo<MenuVO> query(MenuQueryDTO menuQueryDTO);

    MenuVO details(Long id);

    List<MenuVO> findByUserId(Long userId);

    List<MenuVO> findByUserId(Long userId, Collection<Long> roleIds);
}
