package com.dp.ai_code_agent.core.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.dp.ai_code_agent.common.exception.BusinessException;
import com.dp.ai_code_agent.common.exception.ErrorCode;
import com.dp.ai_code_agent.core.converter.DemoUserConverter;
import com.dp.ai_code_agent.core.dto.DemoUserDTO;
import com.dp.ai_code_agent.core.mapper.DemoUserMapper;
import com.dp.ai_code_agent.core.model.DemoUser;
import com.dp.ai_code_agent.core.service.DemoUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 示例业务实现
 */
@Service
@RequiredArgsConstructor
public class DemoUserServiceImpl implements DemoUserService {

    private final DemoUserMapper demoUserMapper;

    private final DemoUserConverter demoUserConverter;

    @Override
    public DemoUserDTO getById(Long id) {
        DemoUser demoUser = QueryChain.of(demoUserMapper)
                .eq(DemoUser::getId, id)
                .get();
        if (demoUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + id);
        }
        return demoUserConverter.toDto(demoUser);
    }

    @Override
    public List<DemoUserDTO> listAll() {
        return demoUserConverter.toDtoList(QueryChain.of(demoUserMapper).list());
    }
}