package com.atlas.user.service;


import com.atlas.user.domain.dto.UserProfileDTO;
import com.atlas.user.domain.vo.AuthInfoVO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.UserInfoVO;
import com.atlas.user.domain.vo.UserVO;
import com.atlas.user.enums.AuthorityDomain;

import java.util.List;

public interface ProfileService {

    UserVO userInfo(Long userId);

    AuthInfoVO getPermissions(Long userId, AuthorityDomain domain);

    void changeUserProfile(Long userId,UserProfileDTO userProfileDTO);

    List<OrgMemberVO> getMyTeam(Long userId);

}
