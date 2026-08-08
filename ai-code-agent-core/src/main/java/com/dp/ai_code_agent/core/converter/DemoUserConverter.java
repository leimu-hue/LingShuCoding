package com.dp.ai_code_agent.core.converter;

import com.dp.ai_code_agent.core.dto.DemoUserDTO;
import com.dp.ai_code_agent.core.model.DemoUser;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 示例：实体与 DTO 转换
 */
@Mapper(componentModel = "spring")
public interface DemoUserConverter {

    DemoUserDTO toDto(DemoUser demoUser);

    List<DemoUserDTO> toDtoList(List<DemoUser> demoUsers);
}