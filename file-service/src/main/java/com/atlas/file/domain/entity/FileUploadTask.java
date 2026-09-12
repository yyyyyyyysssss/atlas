package com.atlas.file.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.file.enums.FileUploadTaskStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 9:08
 */

@Getter
@Setter
@TableName("file_upload_task")
@Builder
public class FileUploadTask extends BaseEntity {

    @Tolerate
    public FileUploadTask(){

    }

    @TableField("file_id")
    private Long fileId;

    @TableField("upload_id")
    private String uploadId;

    @TableField("object_name")
    private String objectName;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("total_size")
    private Long totalSize;

    @TableField("total_chunk")
    private Integer totalChunk;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("status")
    private FileUploadTaskStatus status;



}
