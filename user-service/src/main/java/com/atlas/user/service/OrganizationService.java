package com.atlas.user.service;


import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.dto.UserOrgDTO;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.OrganizationVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * (Organization)表服务接口
 *
 * @author ys
 * @since 2026-02-28 16:21:31
 */
public interface OrganizationService extends IService<Organization> {

    OrganizationVO findById(Long id);

    List<OrganizationVO> findSubUnits(Long id, String organizationType);

    List<OrgMemberVO> findMembers(Long id, String mode);

    void updateMembers(Long id, UserOrgDTO userOrgDTO);

    List<OrganizationVO> findAll();

    List<OrganizationVO> tree(List<String> orgTypes);

    Long createOrganization(OrganizationCreateDTO createDTO);

    void updateOrganization(OrganizationUpdateDTO updateDTO, boolean isFullUpdate);

    void addMembers(Long orgId,List<UserOrgDTO> userOrgList);

    void removeMembers(Long orgId, List<Long> userOrgIds);

    OrganizationVO orgMemberMainCheck(Long id, Long userId);
}

