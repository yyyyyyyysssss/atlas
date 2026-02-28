package com.atlas.user.service.impl;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.vo.OrganizationVO;
import com.atlas.user.mapper.OrganizationMapper;
import com.atlas.user.mapping.OrganizationMapping;
import com.atlas.user.service.OrganizationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * (Organization)表服务实现类
 *
 * @author ys
 * @since 2026-02-28 16:21:32
 */
@Service("organizationService")
@AllArgsConstructor
@Slf4j
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {
    
    private OrganizationMapper organizationMapper;

    @Override
    public OrganizationVO findById(Long id){
        Organization entity = checkAndResult(id);
        return OrganizationMapping.INSTANCE.toOrganizationVO(entity);
    }

    @Override
    public List<OrganizationVO> tree() {
        QueryWrapper<Organization> organizationQueryWrapper = new QueryWrapper<>();
        organizationQueryWrapper
                .lambda()
                .orderByAsc(Organization::getCreateTime);
        List<Organization> organizations = organizationMapper.selectList(organizationQueryWrapper);
        List<OrganizationVO> organizationList = OrganizationMapping.INSTANCE.toOrganizationVO(organizations);
        return TreeUtils.buildTree(
                organizationList,
                OrganizationVO::getId,
                OrganizationVO::getParentId,
                OrganizationVO::setChildren,
                CommonConstant.ROOT_PARENT_ID
        );
    }

    @Override
    @Transactional
    public Long createOrganization(OrganizationCreateDTO createDTO){
        Organization entity = OrganizationMapping.INSTANCE.toOrganization(createDTO);
        entity.setId(IdGen.genId());
        int row = organizationMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateOrganization(OrganizationUpdateDTO updateDTO, boolean isFullUpdate){
        Organization entity = checkAndResult(updateDTO.getId());
        if(isFullUpdate){
            OrganizationMapping.INSTANCE.overwriteOrganization(updateDTO, entity);
        } else {
            OrganizationMapping.INSTANCE.updateOrganization(updateDTO, entity);
        }
        int row = organizationMapper.updateById(entity);
        if (row <= 0) {
            throw new BusinessException("修改失败");
        }
    }

    @Override
    @Transactional
    public void deleteOrganization(Long id){
        checkAndResult(id);
        organizationMapper.deleteById(id);
    }
    
    private Organization checkAndResult(Long id) {
        Organization entity = organizationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }
    
}

