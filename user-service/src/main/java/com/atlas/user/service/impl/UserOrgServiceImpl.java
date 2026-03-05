package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.config.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atlas.user.mapper.UserOrgMapper;
import com.atlas.user.mapping.UserOrgMapping;
import com.atlas.user.domain.entity.UserOrg;
import com.atlas.user.service.UserOrgService;
import com.atlas.user.domain.dto.UserOrgDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;


/**
 * (UserOrg)表服务实现类
 *
 * @author ys
 * @since 2026-03-03 11:08:40
 */
@Service("userOrgService")
@AllArgsConstructor
@Slf4j
public class UserOrgServiceImpl extends ServiceImpl<UserOrgMapper, UserOrg> implements UserOrgService {

    private UserOrgMapper userOrgMapper;

    @Override
    @Transactional
    public void addUserOrg(List<UserOrgDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        // 处理is_main为null的情况
        List<UserOrgDTO> isMainNoNList = list.stream().filter(f -> f.getIsMain() == null).toList();
        if (!isMainNoNList.isEmpty()) {
            Set<Long> userIds = isMainNoNList.stream().map(UserOrgDTO::getUserId).collect(Collectors.toSet());
            // 查询出用户主部门
            List<UserOrg> usersWithMain = this.lambdaQuery()
                    .in(UserOrg::getUserId, userIds)
                    .eq(UserOrg::getIsMain, 1)
                    .list();
            Set<Long> assignedInThisBatch = new HashSet<>();
            for (UserOrgDTO dto : list) {
                if (dto.getIsMain() == null) {
                    // 如果用户没有设置过主部门则默认设置为主部门
                    if(usersWithMain.stream().noneMatch(m -> m.getUserId().equals(dto.getUserId()))){
                        // 如果数据库里没有，且本批次前面也没设过，才设为 1
                        dto.setIsMain(!assignedInThisBatch.contains(dto.getUserId()));
                    } else {
                        // 已设置过主部门的
                        UserOrg userOrg = usersWithMain
                                .stream()
                                .filter(m -> m.getUserId().equals(dto.getUserId()) && m.getOrgId().equals(dto.getOrgId()))
                                .findFirst().orElse(null);
                        // 同组织则直接赋值
                        if(userOrg != null){
                            dto.setIsMain(userOrg.getIsMain());
                        } else { // 不同组织表示用户已经设置过
                            dto.setIsMain(false);
                        }
                    }
                }
                if (Boolean.TRUE.equals(dto.getIsMain())) {
                    assignedInThisBatch.add(dto.getUserId());
                }
            }
        }

        // 检查本次输入中，是否存在同一个用户被设置了多个 is_main=1
        checkMultiMainInRequest(list);

        // 处理主归属冲突：如果要设为新主部门，先将该用户旧的所有主部门标记置为 0
        List<Long> userIdsToReset = list.stream()
                .filter(UserOrgDTO::getIsMain)
                .map(UserOrgDTO::getUserId)
                .distinct()
                .toList();
        if (!userIdsToReset.isEmpty()) {
            this.lambdaUpdate()
                    .in(UserOrg::getUserId, userIdsToReset)
                    .set(UserOrg::getIsMain, 0)
                    .update();
        }

        List<UserOrg> existingList = userOrgMapper.selectByPairs(list);
        Map<String, UserOrg> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        uo -> uo.getUserId() + "_" + uo.getOrgId(),
                        uo -> uo
                ));
        List<UserOrg> finalEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserOrgDTO dto : list) {
            String key = dto.getUserId() + "_" + dto.getOrgId();
            UserOrg entity = existingMap.get(key);
            if (entity != null) {
                // --- 情况 A：记录已存在，仅更新属性 ---
                // 保持原始 ID 和 JoinTime 不变
                entity.setPosId(dto.getPosId() != null ? dto.getPosId() : entity.getPosId());
                entity.setIsMain(dto.getIsMain());
            } else {
                // --- 情况 B：记录不存在，执行初始化 ---
                entity = UserOrgMapping.INSTANCE.toUserOrg(dto);
                entity.setId(IdGen.genId());
                entity.setJoinTime(now);
            }
            finalEntities.add(entity);
        }
        this.saveOrUpdateBatch(finalEntities);
    }

    public UserOrg findUserOrgMain(Long userId){

        return this.lambdaQuery()
                .eq(UserOrg::getUserId, userId)
                .eq(UserOrg::getIsMain, 1)
                .one();
    }

    public void deleteOrgUser(Long orgId, List<Long> userOrgIds) {
        if (orgId == null || CollectionUtils.isEmpty(userOrgIds)) {
            return;
        }
        LambdaQueryWrapper<UserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOrg::getOrgId, orgId)
                .in(UserOrg::getId, userOrgIds);
        int deletedRows = userOrgMapper.delete(wrapper);
        log.info("从组织 {} 中移除了 {} 名成员", orgId, deletedRows);
    }

    @Override
    public List<UserOrgDTO> findByUserId(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        List<UserOrg> list = this.lambdaQuery()
                .in(UserOrg::getUserId, userIds)
                .list();
        return UserOrgMapping.INSTANCE.toUserOrgDTO(list);
    }

    @Override
    public List<UserOrgDTO> findByOrgId(Collection<Long> orgIds) {
        if (CollectionUtils.isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        List<UserOrg> list = this.lambdaQuery()
                .in(UserOrg::getOrgId, orgIds)
                .list();
        return UserOrgMapping.INSTANCE.toUserOrgDTO(list);
    }

    private void checkMultiMainInRequest(List<UserOrgDTO> list) {
        // 校验同一个 userId 在 list 中是否有多个 isMain = 1
        Map<Long, Long> mainCountMap = list.stream()
                .filter(UserOrgDTO::getIsMain)
                .collect(Collectors.groupingBy(UserOrgDTO::getUserId, Collectors.counting()));
        mainCountMap.forEach((userId, count) -> {
            if (count > 1) {
                throw new BusinessException("用户[" + userId + "]不能同时设置多个主归属");
            }
        });
    }

}

