package com.dp.ai_code_agent.web.controller;

import com.dp.ai_code_agent.common.result.Result;
import com.dp.ai_code_agent.core.dto.DemoUserDTO;
import com.dp.ai_code_agent.core.service.DemoUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 示例控制器
 */
@RestController
@RequestMapping("/api/demo-users")
@RequiredArgsConstructor
public class DemoUserController {

    private final DemoUserService demoUserService;

    @GetMapping("/{id}")
    public Result<DemoUserDTO> getById(@PathVariable Long id) {
        return Result.ok(demoUserService.getById(id));
    }

    @GetMapping
    public Result<List<DemoUserDTO>> listAll() {
        return Result.ok(demoUserService.listAll());
    }
}