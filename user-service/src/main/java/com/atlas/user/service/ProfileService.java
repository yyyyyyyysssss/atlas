package com.atlas.user.service;


import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.dto.ChangeUsernameDTO;
import com.atlas.user.domain.dto.UserProfileDTO;
import com.atlas.user.domain.vo.AuthInfoVO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.UserInfoVO;

import java.util.List;

public interface ProfileService {

    UserInfoVO userInfo(Long userId);

    AuthInfoVO authInfo(Long userId);

    void changeUserProfile(Long userId,UserProfileDTO userProfileDTO);

    List<OrgMemberVO> getMyTeam(Long userId);

    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    void changeUsername(Long userId, ChangeUsernameDTO changeUsernameDTO);

}
