package com.atlas.user.service;

import com.atlas.user.domain.dto.UserCreateDTO;
import com.atlas.user.domain.dto.UserQueryDTO;
import com.atlas.user.domain.dto.UserUpdateDTO;
import com.atlas.user.domain.entity.User;
import com.atlas.user.domain.vo.UserCreateVO;
import com.atlas.user.domain.vo.UserVO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface UserService extends IService<User> {

    boolean saveOrUpdate(User user);

    User findByUsername(String username);

    User findByUserId(Serializable userId);

    List<UserVO> findByUserId(Collection<Long> userIds);

    List<UserVO> findByEmail(Collection<String> emails);

    List<UserVO> findByPhone(Collection<String> phones);

    List<UserVO> findByRoleId(Long roleId);

    <R> List<UserVO> findListBy(SFunction<User, R> column, Object... value);


    UserCreateVO createUser(UserCreateDTO userCreateDTO);

    Boolean updateUser(UserUpdateDTO userUpdateDTO, Boolean isFullUpdate);

    String resetPassword(Long userId);

    PageInfo<UserVO> queryList(UserQueryDTO queryDTO);

    UserVO details(Long id);

    PageInfo<UserVO> search(Integer pageNum, Integer pageSize, String name, List<Long> ids);

    Boolean bindRoles(Long id, List<Long> roleIds);

    List<UserVO> listUserOptions();

    Boolean deleteUser(Long id);

}
