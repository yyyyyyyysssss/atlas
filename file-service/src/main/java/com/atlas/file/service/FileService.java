package com.atlas.file.service;


import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.FileRangeDTO;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileStreamVO;
import com.atlas.file.domain.vo.FileUploadChunkVO;
import com.atlas.file.domain.vo.FileUploadProgressVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/19 10:11
 */
public interface FileService {

    String checkMD5(String md5);

    String pathSeparator();

    String getUploadId(FileInfoDTO fileInfoDTO);

    FileUploadChunkVO uploadChunk(FileChunkDTO fileChunkDTO);

    FileUploadProgressVO getUploadProgress(String uploadId);

    String getAccessUrl(String uploadId);

    String generateTemporaryUrl(String uploadId, Duration duration);

    String uploadSingleFile(MultipartFile file);

    String uploadSingleFile(InputStream inputStream, String fileName, String fileType);

    InputStream download(String bucketName, String objectName);

    FileStreamVO getFileStream(String bucketName, String objectName, FileRangeDTO range);

    FileInfoVO getFileInfo(String bucketName, String objectName);

}
