package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.entity.User;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.domain.vo.MenuVO;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.domain.vo.UserInfoVO;
import com.atlas.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/10 9:10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final MenuService menuService;

    private final AuthorityService authorityService;

    private final UserService userService;

    private final RoleService roleService;

    @Override
    public UserInfoVO userInfo(Long userId) {
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setFullName(user.getFullName());
        userInfoVO.setAvatar(user.getAvatar());

        // 用户角色
        List<RoleVO> roles = roleService.findByUserId(userId);
        if (!CollectionUtils.isEmpty(roles)){
            List<String> roleCodes = roles.stream().map(RoleVO::getCode).toList();
            userInfoVO.setRoleCodes(roleCodes);
        }else {
            userInfoVO.setRoleCodes(Collections.emptyList());
        }
        List<Long> roleIds = Optional.ofNullable(roles).orElse(new ArrayList<>()).stream().map(RoleVO::getId).toList();

        // 用户菜单
        List<MenuVO> menus = menuService.findByUserId(userId, roleIds);
        if (!CollectionUtils.isEmpty(menus)){
            List<MenuVO> menuTree = TreeUtils.buildTree(
                    menus,
                    MenuVO::getId,
                    MenuVO::getParentId,
                    MenuVO::setChildren,
                    0L
            );
            userInfoVO.setMenuTree(menuTree);
        }else {
            userInfoVO.setMenuTree(Collections.emptyList());
        }

        // 用户权限
        List<AuthorityVO> authorityVOList = authorityService.findByUserId(userId);
        if (!CollectionUtils.isEmpty(authorityVOList)){
            List<String> permissionCodes = authorityVOList.stream().map(AuthorityVO::getCode).distinct().toList();
            userInfoVO.setPermissionCodes(permissionCodes);
        }else {
            userInfoVO.setPermissionCodes(Collections.emptyList());
        }

        return userInfoVO;
    }

    @Override
    public Boolean changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
//        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
//        userQueryWrapper
//                .lambda()
//                .select(User::getId, User::getPassword)
//                .eq(User::getId, userId);
//        User user = userService.getOne(userQueryWrapper);
//        if (user == null){
//            throw new BusinessException("用户不存在");
//        }
//        if (!passwordEncoder.matches(changePasswordDTO.getOriginPassword(), user.getPassword())){
//            throw new BusinessException("原密码不正确");
//        }
//        if(passwordEncoder.matches(changePasswordDTO.getNewPassword(), user.getPassword())){
//            throw new BusinessException("新密码不能与原密码相同");
//        }
//        String newEncodedPassword = passwordEncoder.encode(changePasswordDTO.getNewPassword());
//        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
//        userUpdateWrapper
//                .lambda()
//                .set(User::getPassword, newEncodedPassword)
//                .eq(User::getId, userId);
//        return userService.update(userUpdateWrapper);
        return false;
    }

    @Override
    public Boolean changeAvatar(Long userId, String avatarUrl) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .lambda()
                .select(User::getId)
                .eq(User::getId, userId);
        User user = userService.getOne(userQueryWrapper);
        if (user == null){
            throw new BusinessException("用户不存在");
        }
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .lambda()
                .set(User::getAvatar, avatarUrl)
                .eq(User::getId, userId);
        return userService.update(userUpdateWrapper);
    }
}
