package com.atlas.user.service;

import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.user.domain.dto.UserCreateDTO;
import com.atlas.user.domain.dto.UserQueryDTO;
import com.atlas.user.domain.dto.UserUpdateDTO;
import com.atlas.user.domain.entity.User;
import com.atlas.user.domain.vo.UserCreateVO;
import com.atlas.user.domain.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface UserService extends IService<User> {

    boolean saveOrUpdate(User user);

    Long createCoreUser(CreateUserSpec userSpec);

    UserAuthDTO loadUserByUserId(Long id);

    User findByUserId(Serializable userId);

    List<UserVO> findByRoleId(Long roleId);

    UserCreateVO createUser(UserCreateDTO userCreateDTO);

    Boolean updateUser(UserUpdateDTO userUpdateDTO, Boolean isFullUpdate);

    PageInfo<UserVO> queryList(UserQueryDTO queryDTO);

    UserVO details(Long id);

    PageInfo<UserVO> search(Integer pageNum, Integer pageSize, String name, List<Long> ids);

    Boolean bindRoles(Long id, List<Long> roleIds);

    List<UserVO> listUserOptions();

    Boolean deleteUser(Long id);

    String resetPassword(Long id);

}
