package com.dp.ai_code_agent.core.service;

import com.dp.ai_code_agent.core.dto.DemoUserDTO;

import java.util.List;

/**
 * 示例业务接口
 */
public interface DemoUserService {

    DemoUserDTO getById(Long id);

    List<DemoUserDTO> listAll();
}