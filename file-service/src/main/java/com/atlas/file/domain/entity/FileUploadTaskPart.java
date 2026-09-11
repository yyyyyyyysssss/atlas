package com.atlas.file.domain.entity;

import com.atlas.common.mybatis.entity.BaseIdEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 9:08
 */

@Getter
@Setter
@TableName("file_upload_task_part")
@Builder
public class FileUploadTaskPart extends BaseIdEntity {

    @Tolerate
    public FileUploadTaskPart(){

    }

    @TableField("task_id")
    private Long taskId;

    @TableField("upload_id")
    private String uploadId;

    @TableField("part_number")
    private Integer partNumber;

    @TableField("part_size")
    private Long partSize;

    @TableField("part_etag")
    private String partEtag;

    @TableField("create_time")
    private LocalDateTime createTime;

}
