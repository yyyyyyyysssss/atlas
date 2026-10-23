package com.atlas.auth.service;

import com.atlas.auth.domain.dto.ApplicationQueryDTO;
import com.atlas.auth.domain.dto.ApplicationSaveDTO;
import com.atlas.auth.domain.entity.Application;
import com.atlas.auth.domain.vo.ApplicationVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ApplicationService extends IService<Application> {

    Application getActive(Long id);

    Application getRequiredByIdAndProjectId(Long id, Long projectId);

    List<Application> findByProjectId(Long projectId);

    Long saveInfo(String projectCode, ApplicationSaveDTO saveDTO);

    boolean updateEnabled(String projectCode, Long id, Boolean enabled);

    PageInfo<ApplicationVO> getPage(String projectCode, ApplicationQueryDTO queryDTO);

    ApplicationVO detail(String projectCode, Long applicationId);

}
