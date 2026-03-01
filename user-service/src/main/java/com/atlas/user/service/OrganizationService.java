package com.atlas.user.service;


import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationQueryDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.vo.OrganizationVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;


/**
 * (Organization)表服务接口
 *
 * @author ys
 * @since 2026-02-28 16:21:31
 */
public interface OrganizationService extends IService<Organization> {

    OrganizationVO findById(Long id);

    List<OrganizationVO> tree(List<String> orgTypes);

    Long createOrganization(OrganizationCreateDTO createDTO);

    void updateOrganization(OrganizationUpdateDTO updateDTO, boolean isFullUpdate);

    void deleteOrganization(Long id);
}

