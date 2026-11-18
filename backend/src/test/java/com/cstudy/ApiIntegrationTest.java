package com.cstudy;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cstudy.auth.domain.AdminModel;
import com.cstudy.auth.repository.LoginRepository;
import com.cstudy.common.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AppProperties properties;
    @Autowired
    LoginRepository loginRepository;

    // ---------- 분류 · 콘텐츠 ----------

    @Test
    void 대분류는_6개이고_자료구조만_활성이다() throws Exception {
        mvc.perform(get("/api/major-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(6)))
                .andExpect(jsonPath("$.items[0].name").value("자료구조"))
                .andExpect(jsonPath("$.items[0].active").value(true))
                .andExpect(jsonPath("$.items[?(@.active == true)]", hasSize(1)));
    }

    @Test
    void 자료구조의_중분류는_스택과_큐다() throws Exception {
        mvc.perform(get("/api/major-categories/1/medium-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path[0].name").value("자료구조"))
                .andExpect(jsonPath("$.items[0].name").value("스택"))
                .andExpect(jsonPath("$.items[1].name").value("큐"))
                .andExpect(jsonPath("$.overview").doesNotExist());
    }

    @Test
    void 소분류_목록은_중분류의_개념_설명을_함께_준다() throws Exception {
        mvc.perform(get("/api/medium-categories/7/minor-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview").value(org.hamcrest.Matchers.containsString("스택이란?")))
                .andExpect(jsonPath("$.items[0].name").value("Push"));
        mvc.perform(get("/api/medium-categories/8/minor-categories"))
                .andExpect(jsonPath("$.overview").value(org.hamcrest.Matchers.containsString("큐란?")));
    }

    @Test
    void 하위_분류가_없으면_빈_목록을_준다() throws Exception {
        mvc.perform(get("/api/major-categories/2/medium-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void 단계가_맞지_않거나_없는_분류는_404() throws Exception {
        mvc.perform(get("/api/major-categories/7/medium-categories")).andExpect(status().isNotFound());
        mvc.perform(get("/api/medium-categories/999/minor-categories")).andExpect(status().isNotFound());
        mvc.perform(get("/api/minor-categories/7/content")).andExpect(status().isNotFound());
    }

    @Test
    void 콘텐츠는_설명과_파일_URL_경로를_준다() throws Exception {
        mvc.perform(get("/api/minor-categories/9/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(startsWith("스택 Push")))
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.videoUrl").value("https://media.example.test/media/animations/stack-push.gif"))
                .andExpect(jsonPath("$.fallbackImageUrl").value("https://media.example.test/media/fallback/stack-push.png"))
                .andExpect(jsonPath("$.path", hasSize(3)));
    }

    // ---------- 조회수 · 통계 ----------

    @Test
    void 조회수를_기록하면_통계에_합산된다() throws Exception {
        String token = login();
        long before = totalViews(token, 8);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/views").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mediumCategoryId\":8}"))
                    .andExpect(status().isNoContent());
        }

        org.junit.jupiter.api.Assertions.assertEquals(before + 3, totalViews(token, 8));
    }

    @Test
    void 조회_기록이_없는_기간은_모든_중분류가_0이다() throws Exception {
        mvc.perform(get("/api/admin/statistics").param("start", "2020-01-01").param("end", "2020-01-31")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats", hasSize(2)))
                .andExpect(jsonPath("$.stats[0].totalViews").value(0))
                .andExpect(jsonPath("$.stats[1].totalViews").value(0));
    }

    @Test
    void 중분류가_아닌_ID의_조회수_기록은_거부한다() throws Exception {
        mvc.perform(post("/api/views").contentType(MediaType.APPLICATION_JSON).content("{\"mediumCategoryId\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/views").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 잘못된_기간은_400() throws Exception {
        String token = login();
        mvc.perform(get("/api/admin/statistics").param("start", "2024-02-01").param("end", "2024-01-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("조회 기간을 다시 선택해 주세요."));
        mvc.perform(get("/api/admin/statistics")
                        .param("start", LocalDate.now().toString())
                        .param("end", LocalDate.now().plusDays(3).toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/statistics").param("start", "어제")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ---------- 인증 · 인가 ----------

    @Test
    void 토큰이_없거나_잘못되면_통계는_401() throws Exception {
        mvc.perform(get("/api/admin/statistics")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mvc.perform(get("/api/admin/statistics").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_토큰과_다른_키로_서명한_토큰은_401() throws Exception {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        String expired = Jwts.builder().subject("tester").claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(secret)).compact();
        String forged = Jwts.builder().subject("tester").claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("another-secret-another-secret-another-secret".getBytes(StandardCharsets.UTF_8)))
                .compact();
        String notAdmin = Jwts.builder().subject("tester").claim("role", "USER")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(secret)).compact();

        for (String token : new String[]{expired, forged, notAdmin}) {
            mvc.perform(get("/api/admin/statistics").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void 로그인_실패는_어느_항목이_틀렸는지_알리지_않는다() throws Exception {
        String expected = "계정이 존재하지 않거나 비밀번호가 잘못되었습니다.";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"tester\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(expected));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"nobody\",\"password\":\"tester-password-1234\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(expected));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비밀번호는_BCrypt_해시로_저장된다() {
        AdminModel admin = loginRepository.findById("tester").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(admin.getPasswordHash().startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertNotEquals("tester-password-1234", admin.getPasswordHash());
    }

    @Test
    void CORS는_등록된_프론트엔드_주소만_허용한다() throws Exception {
        mvc.perform(options("/api/major-categories").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/major-categories").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void 오류_응답에_내부_정보가_없다() throws Exception {
        mvc.perform(post("/api/views").contentType(MediaType.APPLICATION_JSON).content("{잘못된 JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(org.hamcrest.Matchers.containsString("Exception"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ---------- 도우미 ----------

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"tester\",\"password\":\"tester-password-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private long totalViews(String token, long mediumId) throws Exception {
        String body = mvc.perform(get("/api/admin/statistics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (var stat : objectMapper.readTree(body).get("stats")) {
            if (stat.get("mediumCategoryId").asLong() == mediumId) {
                return stat.get("totalViews").asLong();
            }
        }
        throw new AssertionError("중분류가 통계에 없습니다: " + mediumId);
    }
}
