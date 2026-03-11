package com.atlas.user.service.impl;

import com.atlas.common.core.constant.BusinessSequenceConstant;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.SequenceGenerator;
import com.atlas.user.config.idwork.IdGen;
import com.atlas.user.domain.dto.PositionCreateDTO;
import com.atlas.user.domain.dto.PositionQueryDTO;
import com.atlas.user.domain.dto.PositionUpdateDTO;
import com.atlas.user.domain.entity.Position;
import com.atlas.user.domain.vo.OrganizationVO;
import com.atlas.user.domain.vo.PositionVO;
import com.atlas.user.mapper.PositionMapper;
import com.atlas.user.mapping.PositionMapping;
import com.atlas.user.service.OrganizationService;
import com.atlas.user.service.PositionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * (Position)表服务实现类
 *
 * @author ys
 * @since 2026-03-10 14:29:21
 */
@Service("positionService")
@AllArgsConstructor
@Slf4j
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {
    
    private final PositionMapper positionMapper;

    private final SequenceGenerator numberSequenceGenerator;

    private final OrganizationService organizationService;
    
    @Override
    public PageInfo<PositionVO> queryList(PositionQueryDTO queryDTO){
        Long orgId = queryDTO.getOrgId();
        if(orgId != null){
            OrganizationVO organizationVO = organizationService.findById(orgId);
            queryDTO.setOrgPath(organizationVO.getOrgPath());
        }
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        List<Position> positions = positionMapper.queryList(queryDTO);
        if (positions == null || positions.isEmpty()) {
            return new PageInfo<>();
        }
        PageInfo<Position> positionPageInfo = PageInfo.of(positions);
        List<PositionVO> result = PositionMapping.INSTANCE.toPositionVO(positions);
        PageInfo<PositionVO> pageInfo = new PageInfo<>();
        pageInfo.setList(result);
        pageInfo.setTotal(positionPageInfo.getTotal());
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public PositionVO findById(Long id){
        Position entity = checkAndResult(id);
        return PositionMapping.INSTANCE.toPositionVO(entity);
    }

    @Override
    @Transactional
    public Long createPosition(PositionCreateDTO createDTO){
        Position entity = PositionMapping.INSTANCE.toPosition(createDTO);
        entity.setId(IdGen.genId());
        entity.setPosCode(numberSequenceGenerator.generate(BusinessSequenceConstant.POSITION_CODE));
        int row = positionMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updatePosition(PositionUpdateDTO updateDTO, boolean isFullUpdate){
        Position entity = checkAndResult(updateDTO.getId());
        if(isFullUpdate){
            PositionMapping.INSTANCE.overwritePosition(updateDTO, entity);
        } else {
            PositionMapping.INSTANCE.updatePosition(updateDTO, entity);
        }
        int row = positionMapper.updateById(entity);
        if (row <= 0) {
            throw new BusinessException("修改失败");
        }
    }

    @Override
    @Transactional
    public void deletePosition(Long id){
        checkAndResult(id);
        positionMapper.deleteById(id);
    }
    
    private Position checkAndResult(Long id) {
        Position entity = positionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }
    
}

