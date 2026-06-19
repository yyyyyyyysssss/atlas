package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.domain.dto.UserProfileDTO;
import com.atlas.user.domain.entity.*;
import com.atlas.user.domain.vo.*;
import com.atlas.user.mapping.UserMapping;
import com.atlas.user.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserOrgService userOrgService;

    private final OrganizationService organizationService;

    @Override
    public UserVO userInfo(Long userId) {
        User user = userService.findByUserId(userId);
        return UserMapping.INSTANCE.toUserVO(user);
    }

    @Override
    public AuthInfoVO getPermissions(Long userId) {
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
    public void changeUserProfile(Long userId, UserProfileDTO userProfileDTO) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (userProfileDTO.getFullName() != null) {
            user.setFullName(userProfileDTO.getFullName());
        }
        if (userProfileDTO.getAvatar() != null) {
            user.setAvatar(userProfileDTO.getAvatar());
        }
        if (userProfileDTO.getMotto() != null) {
            user.setMotto(userProfileDTO.getMotto());
        }
        if (userProfileDTO.getSettings() != null) {
            UserSetting currentSettings = user.getSettings();
            if (currentSettings == null) {
                currentSettings = new UserSetting();
            }
            UserSetting newSettings = userProfileDTO.getSettings();
            // 合并外观设置 (Appearance)
            if (newSettings.getAppearance() != null) {
                AppearanceSetting appearance = currentSettings.getAppearance();
                if (appearance == null) appearance = new AppearanceSetting();
                // 使用 MapStruct 转换器进行属性拷贝
                UserMapping.INSTANCE.updateAppearanceSetting(newSettings.getAppearance(), appearance);
                currentSettings.setAppearance(appearance);
            }
            // 合并工作台设置 (Workbench/Shortcuts)
            if (newSettings.getWorkbench() != null) {
                WorkbenchSetting workbench = currentSettings.getWorkbench();
                if (workbench == null) workbench = new WorkbenchSetting();
                // 如果传了 shortcuts，直接覆盖
                if (newSettings.getWorkbench().getShortcuts() != null) {
                    workbench.setShortcuts(newSettings.getWorkbench().getShortcuts());
                }
                currentSettings.setWorkbench(workbench);
            }

            // 合并通知设置 (Notification)
            if (newSettings.getNotification() != null) {
                Map<String, Boolean> currentNotification = currentSettings.getNotification();
                if (currentNotification == null) currentNotification = new HashMap<>();
                // putAll 实现 Map 增量更新
                currentNotification.putAll(newSettings.getNotification());
                currentSettings.setNotification(currentNotification);
            }
            user.setSettings(currentSettings);
        }
        userService.updateById(user);
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
}
