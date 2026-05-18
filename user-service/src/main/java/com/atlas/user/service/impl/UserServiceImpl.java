package com.atlas.user.service.impl;

import com.atlas.common.core.api.file.FileApi;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
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
import com.atlas.user.utils.PasswordGeneratorUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final PasswordEncoder passwordEncoder;

    private final RoleAuthorityService roleAuthorityService;

    private final AuthorityService authorityService;

    private final FileApi fileApi;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final String defaultRoleCode = "role_member";

    private static final String ALPHABET = "qazwsxedcrfvtgbyhnujmikolp0987654321";

    private static final int BASE = ALPHABET.length();

    private static final int TARGET_LENGTH = 8;

    @Override
    public UserAuthDTO loadUserByUsername(String username) {
        User user = this.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return getUserAuthDTO(user);
    }

    @Override
    public UserAuthDTO loadUserByUserId(Long id) {
        User user = findByUserId(id);
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
        User u = findByUsername(user.getUsername());
        if (u == null) {
            user.setId(IdGen.genId());
            return userMapper.insert(user) > 0;
        } else {
            user.setId(u.getId());
            return userMapper.updateById(user) > 0;
        }
    }


    @Override
    public User findByUsername(String username) {
        // 先去匹配类型相同的字符串字段 (确保走普通索引)
        LambdaQueryWrapper<User> stringQueryWrapper = new LambdaQueryWrapper<>();
        stringQueryWrapper
                .and(wrapper -> wrapper
                        .eq(User::getUsername, username)
                        .or().eq(User::getPhone, username)
                        .or().eq(User::getEmail, username)
                );
        User user = userMapper.selectOne(stringQueryWrapper);
        // 如果上面没搜到，且入参是纯数字，再尝试根据 ID 查询 (确保走主键索引)
        if (user == null && username.matches("^\\d+$")) {
            try {
                // 转换为 Long 类型，规避隐式转换，确保数字对数字，主键索引绝对生效
                Long userId = Long.parseLong(username);
                user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getId, userId)
                );
            } catch (NumberFormatException e) {
                // 防御性容错：如果数字太大超出了 Long 的范围，直接忽略，不抛异常
            }
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return user;
    }

    @Override
    public User findByUserId(Serializable userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public List<UserDTO> findByIdentifier(Collection<?> identifiers) {

        if (CollectionUtils.isEmpty(identifiers)) return Collections.emptyList();

        Object first = identifiers.iterator().next();

        // 判断是 ID 查询还是 账号查询
        if (first instanceof Long || (first instanceof String && NumberUtils.isDigits((String) first))) {
            return findUsersByField(User::getId, (Collection<Object>) identifiers);
        }
        return findUsersByField(User::getUsername, (Collection<Object>) identifiers);
    }

    @Override
    public List<UserDTO> findByEmail(Collection<String> emails) {

        return findUsersByField(User::getEmail, emails);
    }

    @Override
    public List<UserDTO> findByPhone(Collection<String> phones) {

        return findUsersByField(User::getPhone, phones);
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
        String initPassword = PasswordGeneratorUtils.generate(16);
        userCreateDTO.setPassword(initPassword);
        User user = saveUser(userCreateDTO);
        UserCreateVO userCreateVO = new UserCreateVO();
        userCreateVO.setId(user.getId());
        userCreateVO.setUsername(user.getUsername());
        userCreateVO.setInitialPassword(initPassword);
        return userCreateVO;
    }

    @Override
    @Transactional
    public UserDTO ensureUser(ExternalIdentityDTO externalIdentityDTO) {
        // 身份不存在，尝试通过邮箱/手机号找现有的用户
        String username = StringUtils.firstNonEmpty(externalIdentityDTO.getEmail(), externalIdentityDTO.getPhone());
        if (StringUtils.isEmpty(username)) {
            throw new BusinessException("无法获取用户唯一标识");
        }
        try {
            User user = findByUsername(username);
            return UserMapping.INSTANCE.toUserDTO(user);
        } catch (UsernameNotFoundException e) {
            // 身份不存在，开始注册流程
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setFullName(externalIdentityDTO.getFullName());
            userCreateDTO.setEmail(externalIdentityDTO.getEmail());
            userCreateDTO.setPhone(externalIdentityDTO.getPhone());
            userCreateDTO.setAvatar(externalIdentityDTO.getAvatar());
            Role defaultRole = roleService.findByCode(defaultRoleCode);
            userCreateDTO.setRoleIds(Collections.singletonList(defaultRole.getId()));
            User newUser = this.saveUser(userCreateDTO);
            // 发布事件：通知监听器去下载 Google/GitHub 头像
            if (StringUtils.isNotEmpty(externalIdentityDTO.getAvatar())) {
                log.info("发布头像同步事件, userId: {}", newUser.getId());
                applicationEventPublisher.publishEvent(new UserAvatarSyncEvent(newUser.getId(), externalIdentityDTO.getAvatar()));
            }
            return UserMapping.INSTANCE.toUserDTO(newUser);
        }
    }

    @Transactional
    public User saveUser(UserCreateDTO userCreateDTO) {
        User user = UserMapping.INSTANCE.toUser(userCreateDTO);
        user.setId(IdGen.genId());
        if (user.getPassword() != null) {
            String encryptPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptPassword);
        }
        if (userCreateDTO.getAvatar() == null || userCreateDTO.getAvatar().isEmpty()) {
            String defaultAvatar = generateDefaultAvatar(user.getUsername());
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", key = "#p0.getId()"),
            @CacheEvict(value = "user:authority", key = "#p0.getId()"),
            @CacheEvict(value = "user:menu", key = "#p0.getId()"),
    })
    public Boolean updateUser(UserUpdateDTO userUpdateDTO, Boolean isFullUpdate) {
        User user = checkAndResult(userUpdateDTO.getId());
        if (isFullUpdate) {
            UserMapping.INSTANCE.overwriteUser(userUpdateDTO, user);
        } else {
            UserMapping.INSTANCE.updateUser(userUpdateDTO, user);
        }
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            String defaultAvatar = generateDefaultAvatar(user.getUsername());
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
        return true;
    }

    private String generateDefaultAvatar(String username) {
        Result<String> result = fileApi.generateAvatar(username);
        if (result.isSucceed()) {
            return result.getData();
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
    public String resetPassword(Long userId) {
        checkAndResult(userId);
        String newPassword = PasswordGeneratorUtils.generate(10);
        String encryptPassword = passwordEncoder.encode(newPassword);
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.lambda().eq(User::getId, userId).set(User::getPassword, encryptPassword);
        int update = userMapper.update(null, userUpdateWrapper);
        if (update <= 0) {
            throw new BusinessException("密码重置失败");
        }
        return newPassword;
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
                    .like(User::getUsername, queryDTO.getKeyword())
                    .or()
                    .like(User::getFullName, queryDTO.getKeyword())
                    .or()
                    .like(User::getEmail, queryDTO.getKeyword())
                    .or()
                    .like(User::getPhone, queryDTO.getKeyword());
        }
        if (queryDTO.getEnabled() != null) {
            queryWrapper.eq("enabled", queryDTO.getEnabled());
        }
        queryWrapper.orderByDesc("create_time");
        List<User> users = userMapper.selectList(queryWrapper);
        if (users == null || users.isEmpty()) {
            return new PageInfo<>();
        }
        return toUserVOPageInfo(PageInfo.of(users));
    }

    @Override
    public UserVO details(Long id) {
        User user = checkAndResult(id);
        UserVO userVO = UserMapping.INSTANCE.toUserVO(user);
        // 查询用户对应的角色
        List<RoleVO> roles = roleService.findByUserId(id);
        if (!CollectionUtils.isEmpty(roles)) {
            List<Long> roleIds = roles.stream().map(RoleVO::getId).toList();
            userVO.setRoleIds(roleIds);
        }
        UserOrg userOrgMain = userOrgService.findUserOrgMain(id);
        if (userOrgMain != null) {
            userVO.setOrgId(userOrgMain.getOrgId());
            userVO.setPosId(userOrgMain.getPosId());
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
        return toUserVOPageInfo(PageInfo.of(users));
    }

    private PageInfo<UserVO> toUserVOPageInfo(PageInfo<User> userPageInfo) {
        List<User> users = userPageInfo.getList();
        if (users == null || users.isEmpty()) {
            return new PageInfo<>();
        }
        List<UserVO> result = UserMapping.INSTANCE.toUserVO(users);
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


    /**
     * 通用用户查询抽象
     *
     * @param field  数据库字段对应的 Lambda 表达式
     * @param values 查询的值集合
     * @return UserDTO 列表
     */
    private <T> List<UserDTO> findUsersByField(SFunction<User, T> field, Collection<T> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        List<User> users = this.lambdaQuery()
                .eq(User::getEnabled, true)
                .in(field, values)
                .list();

        return users.stream()
                .map(UserMapping.INSTANCE::toUserDTO)
                .toList();
    }
}
