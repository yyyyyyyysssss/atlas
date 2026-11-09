package com.atlas.user.service;


import com.atlas.common.core.api.user.dto.UserProfileDTO;
import com.atlas.user.domain.dto.ChangeUserProfileDTO;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.PermissionsInfoVO;
import com.atlas.user.enums.AuthorityDomain;

import java.util.List;

public interface ProfileService {

    UserProfileDTO userProfile(Long userId);

    PermissionsInfoVO getPermissions(Long userId, AuthorityDomain domain);

    void changeUserProfile(Long userId, ChangeUserProfileDTO userProfileDTO);

    List<OrgMemberVO> getMyTeam(Long userId);

}
