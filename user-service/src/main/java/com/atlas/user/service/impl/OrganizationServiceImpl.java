package com.atlas.user.service.impl;

import com.atlas.common.core.constant.CommonConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.SequenceGenerator;
import com.atlas.common.core.utils.TreeUtils;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.dto.UserOrgDTO;
import com.atlas.user.domain.entity.Organization;
import com.atlas.user.domain.entity.UserOrg;
import com.atlas.user.domain.vo.OrgMemberVO;
import com.atlas.user.domain.vo.OrganizationVO;
import com.atlas.user.enums.OrganizationStatus;
import com.atlas.user.enums.OrganizationType;
import com.atlas.user.mapper.OrganizationMapper;
import com.atlas.user.mapping.OrganizationMapping;
import com.atlas.user.service.OrganizationService;
import com.atlas.user.service.UserOrgService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private final UserOrgService userOrgService;

    @Override
    @Transactional
    public Long createOrganization(OrganizationCreateDTO createDTO) {
        Organization entity = OrganizationMapping.INSTANCE.toOrganization(createDTO);
        Long id = IdGen.genId();
        entity.setId(id);
        entity.setOrgCode(orgSequenceGenerator.generate());
        if (createDTO.getParentId() != null) {
            Organization parentOrganization = organizationMapper.selectById(createDTO.getParentId());
            if (parentOrganization == null) {
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
    public void updateOrganization(OrganizationUpdateDTO updateDTO, boolean isFullUpdate) {
        Organization entity = checkAndResult(updateDTO.getId());
        String oldPathName = entity.getOrgPathName();
        String newName = updateDTO.getOrgName();
        boolean nameChanged = StringUtils.isNotEmpty(newName) && !newName.equals(entity.getOrgName());
        if (isFullUpdate) {
            OrganizationMapping.INSTANCE.overwriteOrganization(updateDTO, entity);
        } else {
            OrganizationMapping.INSTANCE.updateOrganization(updateDTO, entity);
        }
        if (nameChanged) {
            this.updateOrgPathNameAndChildren(entity, oldPathName);
        } else {
            organizationMapper.updateById(entity);
        }
    }

    @Override
    public void addMembers(Long orgId, List<UserOrgDTO> userOrgList) {
        checkAndResult(orgId);
        userOrgService.addUserOrg(userOrgList);
    }

    @Override
    public void removeMembers(Long orgId, List<Long> userOrgIds) {
        checkAndResult(orgId);
        userOrgService.deleteOrgUser(orgId, userOrgIds);
    }

    @Override
    public OrganizationVO findById(Long id) {
        Organization entity = checkAndResult(id);
        OrganizationVO organizationVO = OrganizationMapping.INSTANCE.toOrganizationVO(entity);
        Long parentId = entity.getParentId();
        if (parentId != CommonConstant.TREE_ROOT_PARENT_ID) {
            Organization parentOrganization = organizationMapper.selectById(parentId);
            if (parentOrganization != null) {
                organizationVO.setParentCode(parentOrganization.getOrgCode());
                organizationVO.setParentName(parentOrganization.getOrgName());
            }
        }
        return organizationVO;
    }

    @Override
    public List<OrganizationVO> findSubUnits(Long id, String organizationType) {
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
                .eq(Organization::getOrgType, organizationType)
                .eq(Organization::getParentId, id)
                .orderByAsc(Organization::getSort)
                .orderByAsc(Organization::getCreateTime);
        List<Organization> deptList = organizationMapper.selectList(organizationQueryWrapper);
        if (CollectionUtils.isEmpty(deptList)) {
            return Collections.emptyList();
        }
        return OrganizationMapping.INSTANCE.toOrganizationVO(deptList);
    }

    @Override
    public List<OrgMemberVO> findMembers(Long id, String mode) {
        Organization organization = checkAndResult(id);
        String targetPath = organization.getOrgPath();
        boolean includeChild;
        switch (mode) {
            case "CHILDREN":
                includeChild = true;
                break;
            case "PARENT":
                includeChild = true;
                targetPath = getParentPath(organization.getOrgPath());
                break;
            case "CURRENT":
            default:
                includeChild = false;
                targetPath = organization.getOrgPath();
                break;
        }
        return organizationMapper.findMembers(targetPath, includeChild);
    }

    @Override
    public List<OrganizationVO> findAll() {
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
                .eq(Organization::getStatus, OrganizationStatus.ACTIVE.getCode())
                .orderByAsc(Organization::getSort)
                .orderByAsc(Organization::getCreateTime);
        List<Organization> organizations = organizationMapper.selectList(organizationQueryWrapper);
        return OrganizationMapping.INSTANCE.toOrganizationVO(organizations);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrgPathNameAndChildren(Organization currentEntity, String oldPathName) {
        String parentPathName = getParentPath(oldPathName);
        String newPathName = StringUtils.isEmpty(parentPathName) ? currentEntity.getOrgName() : parentPathName + currentEntity.getOrgName() + "/";
        currentEntity.setOrgPathName(newPathName);
        organizationMapper.updateById(currentEntity);

        // 获取所有子节点 (不包含自己)
        String orgPath = currentEntity.getOrgPath();
        List<Organization> children = organizationMapper.selectList(
                new LambdaQueryWrapper<Organization>()
                        .likeRight(Organization::getOrgPath, orgPath)
                        .ne(Organization::getId, currentEntity.getId()) // 排除自己
        );
        if (!children.isEmpty()) {
            for (Organization child : children) {
                String childOldPathName = child.getOrgPathName();
                if (StringUtils.isNotEmpty(childOldPathName)) {
                    String newChildPathName = newPathName + childOldPathName.substring(oldPathName.length());
                    child.setOrgPathName(newChildPathName);
                }
            }
            this.updateBatchById(children);
        }

    }

    private String getParentPath(String path) {
        if (path == null || path.length() <= 1) {
            return path; // 或者返回自定义的根标识
        }

        // 1. 去掉末尾可能存在的斜杠，统一成 /1/2/3
        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        // 2. 找到最后一个斜杠的位置
        int lastSlashIndex = normalizedPath.lastIndexOf("/");

        // 3. 如果没找到斜杠，或者斜杠就在开头（如 "/1"），说明已经没有父级了
        if (lastSlashIndex <= 0) {
            // 返回原路径或根路径，取决于你数据库根节点的定义
            // 如果你的根节点是 /1/，那么这里建议直接返回原 path，避免查不到数据
            return path;
        }

        // 4. 截取到父级位置并补回斜杠 -> /1/2/
        return normalizedPath.substring(0, lastSlashIndex + 1);
    }

    // 辅助方法：截取路径获取父级或根级
    private String getRootPath(String path) {
        // 逻辑：/1/2/3/ -> 取第一个斜杠后的 ID
        String[] parts = path.split("/");
        return "/" + parts[1] + "/";
    }

    @Override
    public List<OrganizationVO> tree(List<String> orgTypes) {
        if (CollectionUtils.isEmpty(orgTypes)) {
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
                .in(Organization::getOrgType, orgTypes)
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
    public OrganizationVO orgMemberMainCheck(Long id, Long userId) {
        UserOrg userOrgMain = userOrgService.findUserOrgMain(userId);
        if (userOrgMain == null) {
            return null;
        }
        Long orgId = userOrgMain.getOrgId();
        Organization organization = this.lambdaQuery()
                .select(Organization::getId,Organization::getParentId, Organization::getOrgName, Organization::getOrgPath, Organization::getOrgPathName)
                .eq(Organization::getId, orgId)
                .one();
        if (organization == null) {
            throw new BusinessException("组织部门缺失,请联系管理员");
        }
        if (organization.getId().equals(id) || organization.getParentId().equals(id)) {
            return null;
        }
        return OrganizationMapping.INSTANCE.toOrganizationVO(organization);
    }

    private Organization checkAndResult(Long id) {
        Organization entity = organizationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }

}

