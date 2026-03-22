package com.atlas.user.service;


import com.atlas.user.domain.dto.ChangePasswordDTO;
import com.atlas.user.domain.dto.ShortcutUpdateDTO;
import com.atlas.user.domain.vo.AuthInfoVO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.UserInfoVO;
import com.atlas.user.domain.vo.UserVO;

import java.util.List;

public interface ProfileService {

    UserInfoVO userInfo(Long userId);

    AuthInfoVO authInfo(Long userId);

    List<OrgMemberVO> getMyTeam(Long userId);

    Boolean changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    Boolean changeAvatar(Long userId, String avatarUrl);

    void updateShortcuts(Long userId,List<String> shortcuts);

}
