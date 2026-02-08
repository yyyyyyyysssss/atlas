package com.atlas.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseIdEntity {

    @TableId(value = "id", type = IdType.INPUT)
    protected Long id;

}
