package com.dp.ai_code_agent.web.controller;

import com.dp.ai_code_agent.common.result.PageResult;
import com.dp.ai_code_agent.common.result.Result;
import com.dp.ai_code_agent.user.spi.UserAdminService;
import com.dp.ai_code_agent.user.spi.model.RoleDTO;
import com.dp.ai_code_agent.user.spi.model.UserAdminDTO;
import com.dp.ai_code_agent.web.dto.AssignRolesRequest;
import com.dp.ai_code_agent.web.dto.GrantPermissionsRequest;
import com.dp.ai_code_agent.web.dto.ResetPasswordRequest;
import com.dp.ai_code_agent.web.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口（需 ROLE_ADMIN）。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping("/users")
    public Result<PageResult<UserAdminDTO>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Long roleId) {
        return Result.ok(userAdminService.page(page, size, keyword, status, roleId));
    }

    @GetMapping("/users/{id}")
    public Result<UserAdminDTO> detail(@PathVariable Long id) {
        return Result.ok(userAdminService.detail(id));
    }

    @PatchMapping("/users/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        userAdminService.setStatus(id, req.enabled());
        return Result.ok();
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userAdminService.resetPassword(id, req.newPassword());
        return Result.ok();
    }

    @PutMapping("/users/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest req) {
        userAdminService.assignRoles(id, req.roleIds());
        return Result.ok();
    }

    @GetMapping("/roles")
    public Result<List<RoleDTO>> listRoles() {
        return Result.ok(userAdminService.listRoles());
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> grantPermissions(@PathVariable Long roleId, @Valid @RequestBody GrantPermissionsRequest req) {
        userAdminService.grantPermissions(roleId, req.permissionIds());
        return Result.ok();
    }
}
