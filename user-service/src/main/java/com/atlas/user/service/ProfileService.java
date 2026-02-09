package com.atlas.user.service;


import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.vo.UserInfoVO;

public interface ProfileService {

    UserInfoVO userInfo(Long userId);

    Boolean changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    Boolean changeAvatar(Long userId, String avatarUrl);

}
