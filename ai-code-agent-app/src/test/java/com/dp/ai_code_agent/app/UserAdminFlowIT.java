package com.dp.ai_code_agent.app;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端全链路集成测试（本地 PG + Redis）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminFlowIT {

    @Autowired
    MockMvc mvc;

    private String uniqueUser() {
        return "a" + System.nanoTime();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String register(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id").toString();
    }

    @Test
    void adminAccessAndAuthorization() throws Exception {
        String adminToken = login("admin", "admin123");

        // 管理员可访问管理端
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").isNumber());

        // 普通用户访问管理端 → 403
        String username = uniqueUser();
        register(username, "secret1");
        String userToken = login(username, "secret1");
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void disableUserCascadesSession() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = uniqueUser();
        String userId = register(username, "secret1");
        String userToken = login(username, "secret1");

        // 管理员禁用用户
        mvc.perform(patch("/api/admin/users/" + userId + "/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 用户原 token 立即失效
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPasswordInvalidatesOldPassword() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = uniqueUser();
        String userId = register(username, "secret1");

        mvc.perform(post("/api/admin/users/" + userId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"newsecret1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 旧密码失败，新密码成功
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret1\"}"))
                .andExpect(jsonPath("$.code").value(1002));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"newsecret1\"}"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void grantPermissionTakesEffectAfterRelogin() throws Exception {
        String adminToken = login("admin", "admin123");

        // 取 USER 角色 id 与 user:create / user:view 权限 id
        MvcResult rolesResult = mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String rolesBody = rolesResult.getResponse().getContentAsString();
        Integer userRoleId = first(JsonPath.<List<Number>>read(rolesBody, "$.data[?(@.code=='USER')].id"));
        Integer userViewPermId = first(JsonPath.<List<Number>>read(rolesBody,
                "$.data[?(@.code=='ADMIN')].permissions[?(@.code=='user:view')].id"));
        Integer userCreatePermId = first(JsonPath.<List<Number>>read(rolesBody,
                "$.data[?(@.code=='ADMIN')].permissions[?(@.code=='user:create')].id"));

        // 给 USER 角色授予 user:view + user:create
        mvc.perform(put("/api/admin/roles/" + userRoleId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[" + userViewPermId + "," + userCreatePermId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 新用户登录后身份含 user:create 权限码
        String username = uniqueUser();
        register(username, "secret1");
        String userToken = login(username, "secret1");
        MvcResult meResult = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        List<String> permCodes = JsonPath.read(meResult.getResponse().getContentAsString(), "$.data.permissions[*].code");
        assertThat(permCodes).contains("user:create", "user:view");
    }

    private static Integer first(List<Number> list) {
        return list.get(0).intValue();
    }
}
