package com.atlas.user.service.impl;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.SequenceGenerator;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.vo.OrganizationVO;
import com.atlas.user.enums.OrganizationType;
import com.atlas.user.mapper.OrganizationMapper;
import com.atlas.user.mapping.OrganizationMapping;
import com.atlas.user.service.OrganizationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    private final OrganizationMapper organizationMapper;

    private final SequenceGenerator orgSequenceGenerator;

    @Override
    @Transactional
    public Long createOrganization(OrganizationCreateDTO createDTO){
        Organization entity = OrganizationMapping.INSTANCE.toOrganization(createDTO);
        Long id = IdGen.genId();
        entity.setId(id);
        entity.setOrgCode(orgSequenceGenerator.generate());
        if(createDTO.getParentId() != null){
            Organization parentOrganization = organizationMapper.selectById(createDTO.getParentId());
            if(parentOrganization == null){
                throw new BusinessException("上级组织不存在");
            }
            entity.setOrgPath(parentOrganization.getOrgPath() + id + "/");
            entity.setOrgPathName(parentOrganization.getOrgPathName() + entity.getOrgName() + "/");
        } else {
            entity.setParentId(CommonConstant.TREE_ROOT_PARENT_ID);
            entity.setOrgPath("/" + id + "/");
            entity.setOrgPathName("/" + entity.getOrgName() + "/");
        }
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
    public OrganizationVO findById(Long id){
        Organization entity = checkAndResult(id);
        OrganizationVO organizationVO = OrganizationMapping.INSTANCE.toOrganizationVO(entity);
        Long parentId = entity.getParentId();
        if(parentId != CommonConstant.TREE_ROOT_PARENT_ID){
            Organization parentOrganization = organizationMapper.selectById(parentId);
            if(parentOrganization != null){
                organizationVO.setParentCode(parentOrganization.getOrgCode());
                organizationVO.setParentName(parentOrganization.getOrgName());
            }
        }
        return organizationVO;
    }

    @Override
    public List<OrganizationVO> findSubUnits(Long id, String organizationType){
        QueryWrapper<Organization> organizationQueryWrapper = new QueryWrapper<>();
        organizationQueryWrapper
                .lambda()
                .select(
                        Organization::getId,
                        Organization::getParentId,
                        Organization::getStatus,
                        Organization::getOrgCode,
                        Organization::getOrgName,
                        Organization::getSort)
                .eq(Organization::getOrgType,organizationType)
                .eq(Organization::getParentId,id)
                .orderByAsc(Organization::getSort)
                .orderByAsc(Organization::getCreateTime);
        List<Organization> deptList = organizationMapper.selectList(organizationQueryWrapper);
        if(CollectionUtils.isEmpty(deptList)){
            return Collections.emptyList();
        }
        return OrganizationMapping.INSTANCE.toOrganizationVO(deptList);
    }

    @Override
    public List<OrganizationVO> tree(List<String> orgTypes) {
        if(CollectionUtils.isEmpty(orgTypes)){
            orgTypes = Arrays.stream(OrganizationType.values()).map(OrganizationType::getCode).collect(Collectors.toList());
        }
        QueryWrapper<Organization> organizationQueryWrapper = new QueryWrapper<>();
        organizationQueryWrapper
                .lambda()
                .select(Organization::getId,
                        Organization::getParentId,
                        Organization::getOrgCode,
                        Organization::getOrgName,
                        Organization::getOrgType,
                        Organization::getStatus,
                        Organization::getOrgPath)
                .in(Organization::getOrgType,orgTypes)
                .orderByAsc(Organization::getSort)
                .orderByAsc(Organization::getCreateTime);
        List<Organization> organizations = organizationMapper.selectList(organizationQueryWrapper);
        List<OrganizationVO> organizationList = OrganizationMapping.INSTANCE.toOrganizationVO(organizations);
        return TreeUtils.buildTree(
                organizationList,
                OrganizationVO::getId,
                OrganizationVO::getParentId,
                OrganizationVO::setChildren,
                CommonConstant.TREE_ROOT_PARENT_ID
        );
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

