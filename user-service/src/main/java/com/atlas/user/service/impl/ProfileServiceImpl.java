package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.entity.*;
import com.atlas.user.domain.vo.*;
import com.atlas.user.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    private final PasswordEncoder passwordEncoder;

    private final UserOrgService userOrgService;

    private final OrganizationService organizationService;

    private final PositionService positionService;

    @Override
    public UserInfoVO userInfo(Long userId) {
        User user = userService
                .lambdaQuery()
                .select(User::getFullName, User::getAvatar, User::getSettings)
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUserId(userId);
        userInfoVO.setFullName(user.getFullName());
        userInfoVO.setAvatar(user.getAvatar());
        userInfoVO.setSettings(user.getSettings());
        // 用户所属组织
        UserOrg userOrgMain = userOrgService.findUserOrgMain(userId);
        if (userOrgMain != null) {
            Long orgId = userOrgMain.getOrgId();
            Long posId = userOrgMain.getPosId();

            Organization organization = organizationService
                    .lambdaQuery()
                    .select(Organization::getOrgName, Organization::getOrgPathName)
                    .eq(Organization::getId, orgId)
                    .one();
            if (organization != null) {
                userInfoVO.setOrgId(orgId);
                String orgPathName = organization.getOrgPathName();
                String cleanPath = orgPathName.trim().replaceAll("/$", "");
                String result = Arrays.stream(cleanPath.split("/"))
                        .filter(s -> !s.isEmpty())
                        .skip(1) // 跳过第一层级（集团）
                        .collect(Collectors.joining("-"));
                userInfoVO.setOrgName(result);
            }

            Position position = positionService
                    .lambdaQuery()
                    .select(Position::getPosName)
                    .eq(Position::getId, posId)
                    .one();
            if (position != null) {
                userInfoVO.setPosId(posId);
                userInfoVO.setPosName(position.getPosName());
            }
        }

        return userInfoVO;
    }

    @Override
    public AuthInfoVO authInfo(Long userId) {
        AuthInfoVO authInfoVO = new AuthInfoVO();
        // 用户角色
        List<RoleVO> roles = roleService.findByUserId(userId);
        if (!CollectionUtils.isEmpty(roles)) {
            Set<String> roleCodes = roles.stream().map(RoleVO::getCode).collect(Collectors.toSet());
            authInfoVO.setRoles(roleCodes);
        } else {
            authInfoVO.setRoles(Collections.emptySet());
        }
        List<Long> roleIds = Optional.ofNullable(roles).orElse(new ArrayList<>()).stream().map(RoleVO::getId).toList();

        // 用户菜单
        List<MenuVO> menus = menuService.findByUserId(userId, roleIds);
        if (!CollectionUtils.isEmpty(menus)) {
            List<MenuVO> menuTree = TreeUtils.buildTree(
                    menus,
                    MenuVO::getId,
                    MenuVO::getParentId,
                    MenuVO::setChildren,
                    0L
            );
            authInfoVO.setMenus(menuTree);
        } else {
            authInfoVO.setMenus(Collections.emptyList());
        }

        // 用户权限
        List<AuthorityVO> authorityVOList = authorityService.findByUserId(userId);
        if (!CollectionUtils.isEmpty(authorityVOList)) {
            Set<String> permissionCodes = authorityVOList.stream().map(AuthorityVO::getCode).distinct().collect(Collectors.toSet());
            authInfoVO.setPermissions(permissionCodes);
        } else {
            authInfoVO.setPermissions(Collections.emptySet());
        }
        return authInfoVO;
    }

    @Override
    public List<OrgMemberVO> getMyTeam(Long userId) {
        // 用户所属组织
        UserOrg userOrgMain = userOrgService.findUserOrgMain(userId);
        if (userOrgMain == null) {
            return Collections.emptyList();
        }
        Long orgId = userOrgMain.getOrgId();
        return organizationService.findMembers(orgId, "CHILDREN");
    }

    @Override
    public Boolean changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(changePasswordDTO.getOriginPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(changePasswordDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        String newEncodedPassword = passwordEncoder.encode(changePasswordDTO.getNewPassword());
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .lambda()
                .set(User::getPassword, newEncodedPassword)
                .eq(User::getId, userId);
        return userService.update(userUpdateWrapper);
    }

    @Override
    public Boolean changeAvatar(Long userId, String avatarUrl) {
        User user = userService.findByUserId(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .lambda()
                .set(User::getAvatar, avatarUrl)
                .eq(User::getId, userId);
        return userService.update(userUpdateWrapper);
    }

    @Override
    public void updateShortcuts(Long userId, List<String> shortcuts) {
        User user = userService.
                lambdaQuery()
                .select(User::getId, User::getSettings)
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserSetting settings = user.getSettings();
        if (settings == null) {
            settings = new UserSetting();
        }
        WorkbenchConfig workbench = settings.getWorkbench();
        if (workbench == null) {
            workbench = new WorkbenchConfig();
        }
        workbench.setShortcuts(shortcuts);
        user.setSettings(settings);

        User updateWrapper = new User();
        updateWrapper.setId(userId);
        updateWrapper.setSettings(user.getSettings());

        userService.updateById(updateWrapper);
    }
}
