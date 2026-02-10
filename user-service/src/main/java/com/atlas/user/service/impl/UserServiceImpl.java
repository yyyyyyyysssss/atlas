package com.atlas.user.service.impl;

import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.UserCreateDTO;
import com.atlas.user.domain.dto.UserQueryDTO;
import com.atlas.user.domain.dto.UserUpdateDTO;
import com.atlas.user.domain.entity.User;
import com.atlas.user.domain.entity.UserRole;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.domain.vo.UserCreateVO;
import com.atlas.user.domain.vo.UserVO;
import com.atlas.user.mapper.UserMapper;
import com.atlas.user.mapping.UserMapping;
import com.atlas.user.service.*;
import com.atlas.user.utils.AvatarGeneratorUtils;
import com.atlas.user.utils.PasswordGeneratorUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
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


    @Override
    public boolean saveOrUpdate(User user) {
        User u = findByUsername(user.getUsername());
        if (u == null) {
            user.setId(IdGen.genId());
            user.setCreateTime(LocalDateTime.now());
            return userMapper.insert(user) > 0;
        } else {
            user.setId(u.getId());
            user.setCreateTime(u.getCreateTime());
            return userMapper.updateById(user) > 0;
        }
    }


    @Override
    public User findByUsername(String username) {
        Wrapper<User> queryWrapper = new QueryWrapper<User>().eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public User findByUserId(Serializable userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public List<UserDTO> findByUserId(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            log.warn("findByUserId called with empty userIds");
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectByIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        return users.stream()
                .filter(Objects::nonNull)
                .map(UserMapping.INSTANCE::toUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> findByEmail(Collection<String> emails) {

        return findListBy(User::getEmail,emails.toArray());
    }

    @Override
    public List<UserDTO> findByPhone(Collection<String> phones) {

        return findListBy(User::getPhone,phones);
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
        User user = UserMapping.INSTANCE.toUser(userCreateDTO);
        user.setId(IdGen.genId());
        UserCreateVO userCreateVO = new UserCreateVO();
        String password;
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            password = PasswordGeneratorUtils.generate(10);
            userCreateVO.setInitialPassword(password);
        } else {
            password = user.getPassword();
        }
//        String encryptPassword = passwordEncoder.encode(password);
//        user.setPassword(encryptPassword);
        if (userCreateDTO.getAvatar() == null || userCreateDTO.getAvatar().isEmpty()) {
            String defaultAvatar = generateDefaultAvatar(user.getFullName());
            user.setAvatar(defaultAvatar);
        }
        int row = userMapper.insert(user);
        if (row <= 0) {
            throw new BusinessException("创建用户失败");
        }
        if (userCreateDTO.getRoleIds() != null && !userCreateDTO.getRoleIds().isEmpty()) {
            bindRoles(user.getId(), userCreateDTO.getRoleIds());
        }

        userCreateVO.setId(user.getId());
        return userCreateVO;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user:role", key = "#userUpdateDTO.getId()"),
            @CacheEvict(value = "user:authority", key = "#userUpdateDTO.getId()"),
            @CacheEvict(value = "user:menu", key = "#userUpdateDTO.getId()"),
    })
    public Boolean updateUser(UserUpdateDTO userUpdateDTO, Boolean isFullUpdate) {
        User user = checkAndResult(userUpdateDTO.getId());
        if (isFullUpdate) {
            UserMapping.INSTANCE.overwriteUser(userUpdateDTO, user);
        } else {
            UserMapping.INSTANCE.updateUser(userUpdateDTO, user);
        }
        int i = userMapper.updateById(user);
        if (i <= 0) {
            throw new BusinessException("更新用户失败");
        }
        if (isFullUpdate) {
            bindRoles(user.getId(), userUpdateDTO.getRoleIds());
        } else {
            if (!CollectionUtils.isEmpty(userUpdateDTO.getRoleIds())) {
                bindRoles(user.getId(), userUpdateDTO.getRoleIds());
            }
        }
        return true;
    }

    private String generateDefaultAvatar(String name) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BufferedImage bufferedImage = AvatarGeneratorUtils.generateAvatar(name);
            ImageIO.write(bufferedImage, "png", os);
            InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
            String filename = UUID.randomUUID().toString().replaceAll("-", "");
            return null;
        } catch (IOException e) {
            log.error("生成默认头像失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String resetPassword(Long userId) {
//        checkAndResult(userId);
//        String newPassword = PasswordGeneratorUtils.generate(10);
//        String encryptPassword = passwordEncoder.encode(newPassword);
//        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
//        userUpdateWrapper.lambda().eq(User::getId, userId).set(User::getPassword, encryptPassword);
//        int update = userMapper.update(null, userUpdateWrapper);
//        if (update <= 0) {
//            throw new BusinessException("密码重置失败");
//        }
//        return newPassword;
        return null;
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
        return userVO;
    }

    @Override
    public PageInfo<UserVO> search(Integer pageNum, Integer pageSize, String name, List<Long> ids) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .lambda()
                .select(User::getId, User::getFullName)
                .eq(User::isEnabled, true)
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
                .eq(User::isEnabled, true)
                .orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(userQueryWrapper);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return UserMapping.INSTANCE.toUserVO(users);
    }


    public <R> List<UserDTO> findListBy(SFunction<User, R> column, Object... values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .in(column, values));
        if(CollectionUtils.isEmpty(users)){
            return Collections.emptyList();
        }
        return UserMapping.INSTANCE.toUserDTO(users);
    }
}
