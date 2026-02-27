package com.atlas.user.domain.entity;


import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.common.mybatis.mapper.TreeRelation;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

@Getter
@Setter
@Builder
@TableName("dict_item")
public class DictionaryItem extends BaseEntity implements TreeRelation {

    @Tolerate
    public DictionaryItem(){
    }

    @TableField("dict_id")
    private Long dictId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("label")
    private String label;

    @TableField("value")
    private String value;

    @TableField("alias")
    private String alias;

    @TableField("level")
    private Integer level;

    @TableField("category")
    private String category;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("img_url")
    private String imgUrl;

    @TableField("sort")
    private Integer sort;

    @TableField("ext_json")
    private String extJson;

    @Override
    public String parentFieldName() {
        return "parent_id";
    }

    @Override
    public String childFieldName() {
        return "id";
    }

}
