package com.diplom.two;

import com.diplom.two.dto.UserRq;
import com.diplom.two.dto.UserRs;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class UserLoginTests {

    private String accessToken;
    private String refreshToken;
    private String uniqueEmail;
    private final String existingPassword = "password";
    private final String nonExistingEmail = "nonexisting-user@example.com";
    private final String wrongPassword = "wrongpassword";

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        uniqueEmail = "test" + System.currentTimeMillis() + "@example.com";
    }

    @After
    public void teardown() {
        if (accessToken != null) {
            given()
                    .header("Authorization", accessToken)
                    .when()
                    .delete("/api/auth/user")
                    .then()
                    .statusCode(202);
        }
    }

    @Step("Регистрация пользователя с email: {email}, паролем: {password}, именем: {name}")
    private void registerUser(String email, String password, String name) {
        UserRq requestDTO = new UserRq();
        requestDTO.setEmail(email);
        requestDTO.setPassword(password);
        requestDTO.setName(name);

        given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);
    }

    @Step("Логин пользователя с email: {email} и паролем: {password}")
    private Response loginUser(String email, String password) {
        UserRq loginRequest = new UserRq();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        return given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();
    }

    @Test
    @Step("Тест успешного логина пользователя")
    public void testLoginUserSuccess() {
        registerUser(uniqueEmail, existingPassword, "TestUser");

        Response response = loginUser(uniqueEmail, existingPassword);

        UserRs responseBody = response.getBody().as(UserRs.class);
        accessToken = responseBody.getAccessToken();
        refreshToken = responseBody.getRefreshToken();

        assertEquals(200, response.getStatusCode());
        assertTrue(responseBody.isSuccess());
        assertEquals(uniqueEmail, responseBody.getUser().getEmail());
        assertEquals("TestUser", responseBody.getUser().getName());
        assertFalse(responseBody.getAccessToken().isEmpty());
        assertFalse(responseBody.getRefreshToken().isEmpty());
    }

    @Test
    @Step("Тест логина с неверными учетными данными")
    public void testLoginUserWrongCredentials() {
        Response response = loginUser(nonExistingEmail, wrongPassword);

        assertEquals(401, response.getStatusCode());
        assertFalse(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("email or password are incorrect", response.getBody().jsonPath().getString("message"));
    }
}
