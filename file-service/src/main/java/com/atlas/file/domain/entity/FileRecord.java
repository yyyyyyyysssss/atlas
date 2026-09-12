package com.atlas.file.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.atlas.file.enums.FileStorageType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/19 15:23
 */
@Getter
@Setter
@TableName("file")
@Builder
public class FileRecord extends BaseEntity {

    @Tolerate
    public FileRecord(){

    }

    @TableField("bucket_name")
    private String bucketName;

    @TableField("object_name")
    private String objectName;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("etag")
    private String etag;

    @TableField("access_url")
    private String accessUrl;

    @TableField("original_url")
    private String originalUrl;

    @TableField("md5")
    private String md5;

    @TableField("storage_type")
    private FileStorageType storageType;

}
