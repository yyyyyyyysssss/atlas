package com.atlas.user.service;


import com.atlas.user.domain.dto.PositionCreateDTO;
import com.atlas.user.domain.dto.PositionQueryDTO;
import com.atlas.user.domain.dto.PositionUpdateDTO;
import com.atlas.user.domain.entity.Position;
import com.atlas.user.domain.vo.PositionVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * (Position)表服务接口
 *
 * @author ys
 * @since 2026-03-10 14:29:21
 */
public interface PositionService extends IService<Position> {

    PageInfo<PositionVO> queryList(PositionQueryDTO queryDTO);

    PositionVO findById(Long id);

    List<PositionVO> findByOrgId(Long orgId);

    Long createPosition(PositionCreateDTO createDTO);

    void updatePosition(PositionUpdateDTO updateDTO, boolean isFullUpdate);

    void deletePosition(Long id);
}

