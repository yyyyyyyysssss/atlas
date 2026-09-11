package com.atlas.file.service.impl;

import com.atlas.file.domain.entity.FileUploadTaskPart;
import com.atlas.file.mapper.FileUploadTaskPartMapper;
import com.atlas.file.service.FileUploadTaskPartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 10:30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadTaskPartServiceImpl extends ServiceImpl<FileUploadTaskPartMapper, FileUploadTaskPart> implements FileUploadTaskPartService {
}
