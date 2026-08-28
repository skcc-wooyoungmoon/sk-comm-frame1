package com.sk.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @className    : AdminApiIntegrationTest
 * @description  : JWT 보안 인가 + 주문 멱등성 통합 테스트(MockMvc가 실제 SecurityFilterChain을 통과).
 *                 시드 사용자(admin/manager, 비밀번호 password)를 사용한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_canListUsers() throws Exception {
        String token = login("admin@sk.com", "password");
        mvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void manager_cannotCreateUser_forbidden() throws Exception {
        String token = login("manager@sk.com", "password");
        mvc.perform(post("/api/users").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", "테스트", "email", "mgr-create@sk.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canCreateUser() throws Exception {
        String token = login("admin@sk.com", "password");
        mvc.perform(post("/api/users").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", "생성", "email", "admin-create@sk.com", "role", "USER"))))
                .andExpect(status().isCreated());
    }

    @Test
    void wrongPassword_returns400() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "admin@sk.com", "password", "WRONG"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void order_isIdempotent_sameOrderNoReturnsSameId() throws Exception {
        String token = login("admin@sk.com", "password");
        Map<String, Object> order = Map.of(
                "orderNo", "IT-ORDER-1", "customerId", "C1", "productId", "P1",
                "quantity", 1, "amount", 1000);

        long id1 = placeOrderAndGetId(token, order);
        long id2 = placeOrderAndGetId(token, order); // 동일 orderNo 재요청
        assertThat(id2).isEqualTo(id1); // 멱등: 새 주문이 생기지 않음
    }

    private long placeOrderAndGetId(String token, Map<String, Object> order) throws Exception {
        MvcResult res = mvc.perform(post("/api/orders").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(order)))
                .andReturn();
        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
    }
}
