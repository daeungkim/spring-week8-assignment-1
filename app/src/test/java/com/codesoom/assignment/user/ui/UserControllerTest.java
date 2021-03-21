package com.codesoom.assignment.user.ui;

import com.codesoom.assignment.auth.application.AuthenticationService;
import com.codesoom.assignment.common.BaseControllerTest;
import com.codesoom.assignment.user.application.UserNotFoundException;
import com.codesoom.assignment.user.application.UserService;
import com.codesoom.assignment.user.domain.Role;
import com.codesoom.assignment.user.domain.User;
import com.codesoom.assignment.user.dto.UserModificationData;
import com.codesoom.assignment.user.dto.UserRegistrationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {
    private static final String MY_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjF9.ZZ3CUl0jxeLGvQ1Js5nG2Ty5qGTlqai5ubDMXZOdaDk";
    private static final String OTHER_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjJ9.TEM6MULsZeqkBbUKziCR4Dg_8kymmZkyxsCXlfNJ3g0";
    private static final String ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjEwMDR9.3GV5ZH3flBf0cnaXQCNNZlT4mgyFyBUhn3LKzQohh1A";

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        given(userService.registerUser(any(UserRegistrationData.class)))
                .will(invocation -> {
                    UserRegistrationData registrationData =
                            invocation.getArgument(0);
                    return User.builder()
                            .id(13L)
                            .email(registrationData.getEmail())
                            .name(registrationData.getName())
                            .build();
                });

        given(
                userService.updateUser(
                        eq(1L),
                        any(UserModificationData.class),
                        eq(1L)
                )
        )
                .will(invocation -> {
                    Long id = invocation.getArgument(0);
                    UserModificationData modificationData =
                            invocation.getArgument(1);
                    return User.builder()
                            .id(id)
                            .email("tester@example.com")
                            .name(modificationData.getName())
                            .build();
                });

        given(
                userService.updateUser(
                        eq(100L),
                        any(UserModificationData.class),
                        eq(1L)
                )
        )
                .willThrow(new UserNotFoundException(100L));

        given(
                userService.updateUser(
                        eq(1L),
                        any(UserModificationData.class),
                        eq(2L)
                )
        )
                .willThrow(new AccessDeniedException("Access denied"));

        given(userService.deleteUser(100L))
                .willThrow(new UserNotFoundException(100L));

        given(authenticationService.parseToken(MY_TOKEN)).willReturn(1L);
        given(authenticationService.parseToken(OTHER_TOKEN)).willReturn(2L);
        given(authenticationService.parseToken(ADMIN_TOKEN)).willReturn(1004L);

        given(authenticationService.roles(1L))
                .willReturn(Collections.singletonList(new Role("USER")));
        given(authenticationService.roles(2L))
                .willReturn(Collections.singletonList(new Role("USER")));
        given(authenticationService.roles(1004L))
                .willReturn(List.of(new Role("USER"), new Role("ADMIN")));
    }

    @Test
    void registerUserWithValidAttributes() throws Exception {
        mockMvc.perform(
                RestDocumentationRequestBuilders.
                        post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tester@example.com\"," +
                                "\"name\":\"Tester\",\"password\":\"test\"}")
        )
                .andExpect(status().isCreated())
                .andExpect(content().string(
                        containsString("\"id\":13")
                ))
                .andExpect(content().string(
                        containsString("\"email\":\"tester@example.com\"")
                ))
                .andExpect(content().string(
                        containsString("\"name\":\"Tester\"")
                ))
                .andDo(print())
                .andDo(document("create-user",
                        requestFields(
                                attributes(key("user").value("Fields for user creation")),
                                fieldWithPath("email").type(STRING).description("사용자 이메일")
                                        .attributes(key("constraints").value("최소 세 글자 이상 입력해야합니다.")),
                                fieldWithPath("name").type(STRING).description("사용자 이름")
                                        .attributes(key("constraints").value("한 글자 이상 입력해야합니다.")),
                                fieldWithPath("password").type(STRING).description("사용자 비밀번호")
                                        .attributes(key("constraints").value("비밀번호는 4 ~ 1024 글자 이내 입력해야합니다."))
                        ),
                        responseFields(
                                fieldWithPath("id").type(NUMBER).description("사용자 식별자"),
                                fieldWithPath("email").type(STRING).description("사용자 이메일"),
                                fieldWithPath("name").type(STRING).description("사용자 이름")
                        ))
                );

        verify(userService).registerUser(any(UserRegistrationData.class));
    }

    @Test
    void registerUserWithInvalidAttributes() throws Exception {
        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserWithValidAttributes() throws Exception {
        mockMvc.perform(
                RestDocumentationRequestBuilders.
                        patch("/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"test\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("\"id\":1")
                ))
                .andExpect(content().string(
                        containsString("\"name\":\"TEST\"")
                ))
                .andDo(print())
                .andDo(document("update-user",
                        requestHeaders(headerWithName("Authorization").description("JWT 토큰")),
                        pathParameters(
                                parameterWithName("id").description("사용자 식별자")
                        ),
                        requestFields(
                                attributes(key("user").value("Fields for user creation")),
                                fieldWithPath("name").type(STRING).description("사용자 이름")
                                        .attributes(key("constraints").value("한 글자 이상 입력해야합니다.")),
                                fieldWithPath("password").type(STRING).description("사용자 비밀번호")
                                        .attributes(key("constraints").value("비밀번호는 4 ~ 1024 글자 이내 입력해야합니다."))
                        ),
                        responseFields(
                                fieldWithPath("id").type(NUMBER).description("사용자 식별자"),
                                fieldWithPath("email").type(STRING).description("사용자 이메일"),
                                fieldWithPath("name").type(STRING).description("사용자 이름")
                        ))
                );

        verify(userService)
                .updateUser(eq(1L), any(UserModificationData.class), eq(1L));
    }

    @Test
    void updateUserWithInvalidAttributes() throws Exception {
        mockMvc.perform(
                patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"password\":\"\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserWithNotExsitedId() throws Exception {
        mockMvc.perform(
                patch("/users/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"TEST\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isNotFound());

        verify(userService).updateUser(
                eq(100L),
                any(UserModificationData.class),
                eq(1L));
    }

    @Test
    void updateUserWithoutAccessToken() throws Exception {
        mockMvc.perform(
                patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"test\"}")
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserWithOthersAccessToken() throws Exception {
        mockMvc.perform(
                patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"test\"}")
                        .header("Authorization", "Bearer " + OTHER_TOKEN)
        )
                .andExpect(status().isForbidden());

        verify(userService)
                .updateUser(eq(1L), any(UserModificationData.class), eq(2L));
    }

    @Test
    void destroyWithExistedId() throws Exception {
        mockMvc.perform(
                RestDocumentationRequestBuilders.
                        delete("/users/{id}", 1L)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("delete-user",
                        requestHeaders(headerWithName("Authorization").description("JWT 토큰")),
                        pathParameters(
                                parameterWithName("id").description("사용자 식별자")
                        )
                ));

        verify(userService).deleteUser(1L);
    }

    @Test
    void destroyWithNotExistedId() throws Exception {
        mockMvc.perform(
                delete("/users/100")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
        )
                .andExpect(status().isNotFound());

        verify(userService).deleteUser(100L);
    }

    @Test
    void destroyWithoutAccessToken() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void destroyWithoutAdminRole() throws Exception {
        mockMvc.perform(
                delete("/users/1")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isForbidden());
    }
}