package com.dp.ai_code_agent.user.local.service;

import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.common.result.PageResult;
import com.dp.ai_code_agent.user.local.converter.UserConverter;
import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.dp.ai_code_agent.user.local.model.User;
import com.dp.ai_code_agent.user.local.repository.SessionRepository;
import com.dp.ai_code_agent.user.local.security.PasswordHasher;
import com.dp.ai_code_agent.user.spi.UserAdminService;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import com.dp.ai_code_agent.user.spi.model.UserRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link UserAdminService} 的本地实现：分页 / 详情 / 启停 / 重置密码。
 * <p>
 * 本地仅做最小管理，角色/权限等复杂能力后续经第三方权限管理 SPI 接入。
 */
public class LocalUserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final UserConverter userConverter;

    public LocalUserAdminServiceImpl(UserMapper userMapper, PasswordHasher passwordHasher,
                                     SessionRepository sessionRepository, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.sessionRepository = sessionRepository;
        this.userConverter = userConverter;
    }

    @Override
    public PageResult<UserAdminDTO> page(int page, int size, String keyword, Integer status, UserRole userRole) {
        int total = userMapper.countPage(keyword, status, userRole);
        if (total == 0) {
            return PageResult.of(List.of(), 0, page, size);
        }
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectPage(keyword, status, userRole, offset, size);
        List<UserAdminDTO> records = users.stream().map(userConverter::toUserAdminDTO).toList();
        return PageResult.of(records, total, page, size);
    }

    @Override
    public UserAdminDTO detail(Long id) {
        return userConverter.toUserAdminDTO(requireUser(id));
    }

    @Override
    @Transactional
    public void setStatus(Long id, boolean enabled) {
        User user = requireUser(id);
        user.setStatus(enabled ? 1 : 0);
        userMapper.update(user);
        if (!enabled) {
            sessionRepository.removeAllByUserId(id);
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = requireUser(id);
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userMapper.update(user);
        sessionRepository.removeAllByUserId(id);
    }

    private User requireUser(Long id) {
        User user = userMapper.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
