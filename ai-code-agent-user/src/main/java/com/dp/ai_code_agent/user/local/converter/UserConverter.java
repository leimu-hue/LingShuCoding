package com.dp.ai_code_agent.user.local.converter;

import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.spi.model.PermissionDTO;
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import com.dp.ai_code_agent.user.spi.model.UserIdentity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户实体 ↔ SPI 模型转换器（MapStruct）。
 * <p>
 * 角色/权限由 service 装配后传入，转换器只做字段搬运与状态映射。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "enabled", expression = "java(user.getStatus() != null && user.getStatus() == 1)")
    UserIdentity toUserIdentity(User user, List<RoleDTO> roles, List<PermissionDTO> permissions);

    @Mapping(target = "enabled", expression = "java(user.getStatus() != null && user.getStatus() == 1)")
    UserAdminDTO toUserAdminDTO(User user, List<RoleDTO> roles);
}
