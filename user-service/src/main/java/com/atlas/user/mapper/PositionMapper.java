package com.atlas.user.mapper;


import com.atlas.user.domain.dto.PositionQueryDTO;
import com.atlas.user.domain.entity.Position;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (Position)表数据库访问层
 *
 * @author ys
 * @since 2026-03-10 14:29:21
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    List<Position> queryList(@Param("query") PositionQueryDTO queryDTO);

}

