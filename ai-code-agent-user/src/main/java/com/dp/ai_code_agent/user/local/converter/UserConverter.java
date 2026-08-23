package com.dp.ai_code_agent.user.local.converter;

import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 用户实体 ↔ SPI 模型转换器（MapStruct），只做字段搬运与状态映射。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "enabled", expression = "java(user.getStatus() != null && user.getStatus() == 1)")
    UserIdentity toUserIdentity(User user);

    @Mapping(target = "enabled", expression = "java(user.getStatus() != null && user.getStatus() == 1)")
    UserAdminDTO toUserAdminDTO(User user);
}
