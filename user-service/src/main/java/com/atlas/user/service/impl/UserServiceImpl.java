package com.atlas.user.service.impl;

import com.atlas.common.core.api.auth.AuthApi;
import com.atlas.common.core.api.auth.dto.UserIdentifierCreateDTO;
import com.atlas.common.core.api.auth.dto.UserIdentifierDisplayDTO;
import com.atlas.common.core.api.auth.dto.UserIdentifierUpdateDTO;
import com.atlas.common.core.api.auth.dto.UserPasswordResetDTO;
import com.atlas.common.core.api.file.FileApi;
import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.core.response.Result;
import com.atlas.common.mybatis.enums.DataScope;
import com.atlas.user.domain.dto.UserCreateDTO;
import com.atlas.user.domain.dto.UserOrgDTO;
import com.atlas.user.domain.dto.UserQueryDTO;
import com.atlas.user.domain.dto.UserUpdateDTO;
import com.atlas.user.domain.entity.*;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.domain.vo.UserCreateVO;
import com.atlas.user.domain.vo.UserVO;
import com.atlas.user.event.UserAvatarSyncEvent;
import com.atlas.user.mapper.UserMapper;
import com.atlas.user.mapping.UserMapping;
import com.atlas.user.service.*;
import com.atlas.user.utils.NameGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2023/7/17 11:04
 */
@Service("userService")
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    private final RoleService roleService;

    private final UserRoleService userRoleService;

    private final UserOrgService userOrgService;

    private final OrganizationService organizationService;

    private final RoleAuthorityService roleAuthorityService;

    private final PositionService positionService;

    private final AuthorityService authorityService;

    private final FileApi fileApi;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final AuthApi authApi;

    private final String defaultRoleCode = "role_member";

    @Override
    public UserAuthDTO loadUserByUserId(Long id) {
        User user = getById(id);
        return getUserAuthDTO(user);
    }

    private UserAuthDTO getUserAuthDTO(User user) {
        // 清空用户缓存
        roleService.clearCache(user.getId());
        authorityService.clearCache(user.getId());

        UserAuthDTO userAuthDTO = UserMapping.INSTANCE.toUserAuthDTO(user);

        // 用户所属组织
        UserOrg userOrgMain = userOrgService.findUserOrgMain(user.getId());
        if (userOrgMain != null) {
            userAuthDTO.setOrgId(userOrgMain.getOrgId());
        }

        List<Long> roleIds = userRoleService.findRoleIdByUserId(user.getId());
        if (CollectionUtils.isEmpty(roleIds)) {
            userAuthDTO.setAuthorities(Collections.emptyList());
            userAuthDTO.setDataScopes(Collections.singleton(DataScope.SELF.getCode()));
            return userAuthDTO;
        }
        // 角色关联的数据权限
        Set<Integer> dataScopes = roleService.getDataScope(roleIds);
        userAuthDTO.setDataScopes(dataScopes);
        // 角色关联的权限
        List<Long> authorityIds = roleAuthorityService.findAuthorityIdByRoleId(roleIds);
        if (CollectionUtils.isEmpty(authorityIds)) {
            userAuthDTO.setAuthorities(Collections.emptyList());
            return userAuthDTO;
        }
        List<Authority> authorities = authorityService.listByIds(authorityIds);
        if (CollectionUtils.isEmpty(authorities)) {
            return userAuthDTO;
        }
        List<RoleAuthDTO> list = new ArrayList<>();
        for (Authority authority : authorities) {
            list.add(new RoleAuthDTO(authority.getCode(), authority.getUrls()));
        }
        userAuthDTO.setAuthorities(list);
        return userAuthDTO;
    }

    @Override
    public boolean saveOrUpdate(User user) {
        User u = findByUserId(user.getId());
        if (u == null) {
            user.setId(IdGen.genId());
            return userMapper.insert(user) > 0;
        } else {
            user.setId(u.getId());
            return userMapper.updateById(user) > 0;
        }
    }

    @Override
    public User findByUserId(Serializable userId) {

        return userMapper.selectById(userId);
    }

    // 根据角色查询用户
    @Override
    public List<UserVO> findByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("findByRoleId called with null roleId");
            return Collections.emptyList();
        }
        QueryWrapper<UserRole> userRoleQueryWrapper = new QueryWrapper<>();
        userRoleQueryWrapper
                .lambda()
                .eq(UserRole::getRoleId, roleId);
        List<UserRole> userRoles = userRoleService.list(userRoleQueryWrapper);
        Set<Long> userIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
        List<User> users = userMapper.selectByIds(userIds);
        return UserMapping.INSTANCE.toUserVO(users);
    }

    @Override
    @Transactional
    public UserCreateVO createUser(UserCreateDTO userCreateDTO) {
        User user = saveUser(userCreateDTO);
        // 创建用户标识
        UserIdentifierCreateDTO userIdentifierCreateDTO = new UserIdentifierCreateDTO();
        userIdentifierCreateDTO.setUserId(user.getId());
        userIdentifierCreateDTO.setUsername(userCreateDTO.getUsername());
        userIdentifierCreateDTO.setEmail(userCreateDTO.getEmail());
        userIdentifierCreateDTO.setPhone(userCreateDTO.getPhone());
        Result<UserIdentifierDisplayDTO> responseDTOResult = authApi.createIdentifier(userIdentifierCreateDTO);
        if (!responseDTOResult.isSucceed()) {
            throw new BusinessException("创建账户标识失败: " + responseDTOResult.getMessage());
        }
        UserCreateVO userCreateVO = new UserCreateVO();
        userCreateVO.setId(user.getId());
        UserIdentifierDisplayDTO resultData = responseDTOResult.getData();
        userCreateVO.setInitialPassword(resultData.getInitPassword());
        return userCreateVO;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", key = "#p0.getId()"),
            @CacheEvict(value = "user:authority", key = "#p0.getId()"),
            @CacheEvict(value = "user:menu", key = "#p0.getId()"),
    })
    public Boolean updateUser(UserUpdateDTO userUpdateDTO, Boolean isFullUpdate) {
        User user = checkAndResult(userUpdateDTO.getId());
        UserIdentifierUpdateDTO userIdentifierUpdateDTO = new UserIdentifierUpdateDTO();
        userIdentifierUpdateDTO.setUserId(user.getId());
        if(StringUtils.isNotEmpty(userUpdateDTO.getEmail())){
            userIdentifierUpdateDTO.setEmail(userUpdateDTO.getEmail());
        }
        if(StringUtils.isNotEmpty(userUpdateDTO.getPhone())){
            userIdentifierUpdateDTO.setPhone(userUpdateDTO.getPhone());
        }
        if (isFullUpdate) {
            UserMapping.INSTANCE.overwriteUser(userUpdateDTO, user);
        } else {
            UserMapping.INSTANCE.updateUser(userUpdateDTO, user);
        }
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            String defaultAvatar = generateDefaultAvatar(user.getFullName());
            user.setAvatar(defaultAvatar);
        }
        if (userUpdateDTO.getOrgId() != null) {
            addUserOrg(user.getId(), userUpdateDTO.getOrgId(), userUpdateDTO.getPosId());
        }
        int i = userMapper.updateById(user);
        if (i <= 0) {
            throw new BusinessException("更新用户失败");
        }
        if (isFullUpdate) {
            bindRoles(user.getId(), userUpdateDTO.getRoleIds());
        } else {
            if (userUpdateDTO.getRoleIds() != null) {
                bindRoles(user.getId(), userUpdateDTO.getRoleIds());
            }
        }
        if(StringUtils.isNotEmpty(userIdentifierUpdateDTO.getEmail()) || StringUtils.isNotEmpty(userIdentifierUpdateDTO.getPhone())){
            Result<Void> updateIdentifierResult = authApi.updateIdentifier(userIdentifierUpdateDTO);
            if(!updateIdentifierResult.isSucceed()){
                throw new BusinessException("用户标识更新失败: " + updateIdentifierResult.getMessage());
            }
        }
        return true;
    }

    @Override
    @Transactional
    public Long createCoreUser(CreateUserSpec userSpec) {
        // 身份不存在，开始注册流程
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setFullName(userSpec.fullName());
        userCreateDTO.setAvatar(userSpec.avatarUrl());
        Role defaultRole = roleService.findByCode(defaultRoleCode);
        if (defaultRole != null) {
            userCreateDTO.setRoleIds(Collections.singletonList(defaultRole.getId()));
        }
        // 落库主体表
        User newUser = this.saveUser(userCreateDTO);
        // 发布事件：通知监听器去下载 Google/GitHub 头像
        if (StringUtils.isNotEmpty(userSpec.avatarUrl())) {
            log.info("发布头像同步事件, userId: {}", newUser.getId());
            applicationEventPublisher.publishEvent(new UserAvatarSyncEvent(newUser.getId(), userSpec.avatarUrl()));
        }
        return newUser.getId();
    }

    @Transactional
    public User saveUser(UserCreateDTO userCreateDTO) {
        User user = UserMapping.INSTANCE.toUser(userCreateDTO);
        user.setId(IdGen.genId());
        if(userCreateDTO.getFullName() == null || userCreateDTO.getFullName().isEmpty()){
            String defaultName = NameGenerator.generateFunnyName();
            user.setFullName(defaultName);
        }
        if (userCreateDTO.getAvatar() == null || userCreateDTO.getAvatar().isEmpty()) {
            String defaultAvatar = generateDefaultAvatar(user.getFullName());
            user.setAvatar(defaultAvatar);
        }
        // 添加用户所属组织
        if (userCreateDTO.getOrgId() != null) {
            addUserOrg(user.getId(), userCreateDTO.getOrgId(), userCreateDTO.getPosId());
        }
        int row = userMapper.insert(user);
        if (row <= 0) {
            throw new BusinessException("创建用户失败");
        }
        if (userCreateDTO.getRoleIds() != null && !userCreateDTO.getRoleIds().isEmpty()) {
            bindRoles(user.getId(), userCreateDTO.getRoleIds());
        }
        return user;
    }

    private String generateDefaultAvatar(String fullName) {
        try {
            Result<String> result = fileApi.generateAvatar(fullName);
            if (result.isSucceed()) {
                return result.getData();
            }
        }catch (Exception e){
            log.warn("[{}]头像生成失败", fullName);
        }
        return null;
    }

    private void addUserOrg(Long userId, Long orgId, Long posId) {
        UserOrgDTO userOrgDTO = new UserOrgDTO();
        userOrgDTO.setUserId(userId);
        userOrgDTO.setOrgId(orgId);
        userOrgDTO.setPosId(posId);
        userOrgDTO.setIsMain(Boolean.TRUE);
        userOrgService.addUserOrg(List.of(userOrgDTO));
    }

    @Override
    public PageInfo<UserVO> queryList(UserQueryDTO queryDTO) {
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            queryWrapper
                    .lambda()
                    .like(User::getFullName, queryDTO.getKeyword());
        }
        if (queryDTO.getEnabled() != null) {
            queryWrapper.eq("enabled", queryDTO.getEnabled());
        }
        queryWrapper.orderByDesc("create_time");
        List<User> users = userMapper.selectList(queryWrapper);
        if (users == null || users.isEmpty()) {
            return new PageInfo<>();
        }
        List<Long> userIds = users.stream().map(User::getId).toList();
        Result<List<UserIdentifierDisplayDTO>> listResult = authApi.listByUserId(userIds);
        Map<Long, UserIdentifierDisplayDTO> identifierMap = new HashMap<>();
        if (listResult.isSucceed() && listResult.getData() != null) {
            identifierMap = listResult.getData().stream()
                    .collect(Collectors.toMap(UserIdentifierDisplayDTO::getUserId, dto -> dto));
        }
        return toUserVOPageInfo(PageInfo.of(users),identifierMap);
    }

    @Override
    public UserVO details(Long id) {
        User user = checkAndResult(id);
        UserVO userVO = UserMapping.INSTANCE.toUserVO(user);
        Result<UserIdentifierDisplayDTO> userIdentifierDisplayResult = authApi.getByUserId(id);
        if(userIdentifierDisplayResult.isSucceed() && userIdentifierDisplayResult.getData() != null){
            userVO.setUsername(userIdentifierDisplayResult.getData().getUsername());
            userVO.setEmail(userIdentifierDisplayResult.getData().getEmail());
            userVO.setPhone(userIdentifierDisplayResult.getData().getPhone());
        }
        // 查询用户对应的角色
        List<RoleVO> roles = roleService.findByUserId(id);
        if (!CollectionUtils.isEmpty(roles)) {
            List<Long> roleIds = roles.stream().map(RoleVO::getId).toList();
            userVO.setRoleIds(roleIds);
        }
        UserOrg userOrgMain = userOrgService.findUserOrgMain(id);
        if (userOrgMain != null) {
            Long orgId = userOrgMain.getOrgId();
            Long posId = userOrgMain.getPosId();
            Organization organization = organizationService
                    .lambdaQuery()
                    .select(Organization::getOrgName, Organization::getOrgPathName)
                    .eq(Organization::getId, orgId)
                    .one();
            if (organization != null) {
                userVO.setOrgId(orgId);
                String orgPathName = organization.getOrgPathName();
                String cleanPath = orgPathName.trim().replaceAll("/$", "");
                String result = Arrays.stream(cleanPath.split("/"))
                        .filter(s -> !s.isEmpty())
                        .skip(1) // 跳过第一层级（集团）
                        .collect(Collectors.joining("-"));
                userVO.setOrgName(result);
            }
            Position position = positionService
                    .lambdaQuery()
                    .select(Position::getPosName)
                    .eq(Position::getId, posId)
                    .one();
            if (position != null) {
                userVO.setPosId(posId);
                userVO.setPosName(position.getPosName());
            }
        }
        return userVO;
    }

    @Override
    public PageInfo<UserVO> search(Integer pageNum, Integer pageSize, String name, List<Long> ids) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .lambda()
                .select(User::getId, User::getFullName)
                .eq(User::getEnabled, true)
                .orderByDesc(User::getCreateTime);
        if (name != null && !name.isEmpty()) {
            userQueryWrapper.lambda().like(User::getFullName, name);
        }
        if (ids != null && !ids.isEmpty()) {
            pageSize = ids.size();
            userQueryWrapper.lambda().in(User::getId, ids);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.selectList(userQueryWrapper);
        if (users == null || users.isEmpty()) {
            return new PageInfo<>();
        }
        return toUserVOPageInfo(PageInfo.of(users), Map.of());
    }

    private PageInfo<UserVO> toUserVOPageInfo(PageInfo<User> userPageInfo, Map<Long, UserIdentifierDisplayDTO> identifierMap) {
        List<User> users = userPageInfo.getList();
        if (users == null || users.isEmpty()) {
            return new PageInfo<>();
        }
        List<UserVO> result = users.stream().map(m -> {
            UserVO userVO = UserMapping.INSTANCE.toUserVO(m);
            UserIdentifierDisplayDTO userIdentifierDisplayDTO = identifierMap.get(m.getId());
            if (userIdentifierDisplayDTO != null) {
                userVO.setUsername(userIdentifierDisplayDTO.getUsername());
                userVO.setEmail(userIdentifierDisplayDTO.getEmail());
                userVO.setPhone(userIdentifierDisplayDTO.getPhone());
            }
            return userVO;
        }).toList();
        PageInfo<UserVO> pageInfo = new PageInfo<>();
        pageInfo.setList(result);
        pageInfo.setTotal(userPageInfo.getTotal());
        pageInfo.setPageNum(userPageInfo.getPageNum());
        pageInfo.setPageSize(userPageInfo.getPageSize());
        return pageInfo;
    }

    @Override
    public Boolean deleteUser(Long id) {
        int i = userMapper.deleteById(id);
        if (i <= 0) {
            throw new BusinessException("删除用户失败，用户不存在");
        }
        // 解绑用户对应的角色
        roleService.bindUserRole(id, new ArrayList<>());
        return true;
    }

    @Override
    public String resetPassword(Long id) {
        UserPasswordResetDTO resetDTO = new UserPasswordResetDTO();
        resetDTO.setUserId(id);
        Result<String> result = authApi.resetPassword(resetDTO);
        if (!result.isSucceed()){
            throw new BusinessException("重置失败: " + result.getMessage());
        }
        return result.getData();
    }

    @Override
    @Transactional
    public Boolean bindRoles(Long id, List<Long> roleIds) {

        return roleService.bindUserRole(id, roleIds);
    }

    private User checkAndResult(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public List<UserVO> listUserOptions() {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .lambda()
                .select(User::getId, User::getFullName)
                .eq(User::getEnabled, true)
                .orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(userQueryWrapper);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return UserMapping.INSTANCE.toUserVO(users);
    }
}
