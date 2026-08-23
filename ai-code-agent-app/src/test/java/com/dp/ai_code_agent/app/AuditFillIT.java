package com.dp.ai_code_agent.app;

import com.dp.ai_code_agent.user.local.mapper.UserMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审计字段自动填充与逻辑删除集成测试（本地 PG + Redis）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFillIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserMapper userMapper;

    private String register(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id").toString();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void register_autoFillsAuditFields() throws Exception {
        String username = "audit" + System.nanoTime();
        register(username, "secret1");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_time, update_time, create_at, update_user_id, is_deleted FROM t_user WHERE username = ?",
                username);
        assertThat(row.get("created_time")).isNotNull();
        assertThat(row.get("update_time")).isNotNull();
        assertThat(((Number) row.get("create_at")).longValue()).isZero();
        assertThat(((Number) row.get("update_user_id")).longValue()).isZero();
        assertThat(row.get("is_deleted")).isEqualTo(false);
    }

    @Test
    void adminSetStatus_fillsUpdateUserId() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "audit" + System.nanoTime();
        String userId = register(username, "secret1");

        mvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long adminId = jdbc.queryForObject("SELECT id FROM t_user WHERE username = 'admin'", Long.class);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT update_user_id FROM t_user WHERE id = ?", Long.parseLong(userId));
        assertThat(((Number) row.get("update_user_id")).longValue()).isEqualTo(adminId);
    }

    @Test
    void logicDelete_marksDeletedAndFiltersQuery() throws Exception {
        String username = "audit" + System.nanoTime();
        String userId = register(username, "secret1");

        userMapper.deleteById(Long.parseLong(userId));

        Boolean deleted = jdbc.queryForObject(
                "SELECT is_deleted FROM t_user WHERE id = ?", Boolean.class, Long.parseLong(userId));
        assertThat(deleted).isTrue();
        assertThat(userMapper.selectByUsername(username)).isNull();
    }
}
