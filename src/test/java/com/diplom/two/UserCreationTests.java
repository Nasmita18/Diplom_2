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

public class UserCreationTests {

    private String accessToken;
    private String refreshToken;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
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

    @Test
    @Step("Тест успешного создания пользователя")
    public void testCreateUserSuccess() {
        UserRq requestDTO = new UserRq();
        String uniqueEmail = "test-data" + System.currentTimeMillis() + "@yandex.ru";

        requestDTO.setEmail(uniqueEmail);
        requestDTO.setPassword("password123");
        requestDTO.setName("Username");

        Response response = given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .extract()
                .response();

        UserRs responseBody = response.getBody().as(UserRs.class);

        accessToken = responseBody.getAccessToken();
        refreshToken = responseBody.getRefreshToken();

        assertEquals(200, response.getStatusCode());
        assertTrue(responseBody.isSuccess());
        assertEquals(uniqueEmail, responseBody.getUser().getEmail());
        assertEquals("Username", responseBody.getUser().getName());
        assertFalse(responseBody.getAccessToken().isEmpty());
        assertFalse(responseBody.getRefreshToken().isEmpty());
    }

    @Test
    @Step("Тест создания уже существующего пользователя")
    public void testCreateUserAlreadyExists() {
        UserRq requestDTO = new UserRq();
        String email = "existing-user" + System.currentTimeMillis() + "@example.com";
        String password = "password";
        String name = "ExistingUser";

        // Регистрируем пользователя
        registerUser(requestDTO, email, password, name);

        // Попытка зарегистрировать того же пользователя еще раз
        attemptToRegisterUserAgain(requestDTO);
    }

    @Step("Регистрация пользователя с email: {email}, паролем: {password}, именем: {name}")
    private void registerUser(UserRq requestDTO, String email, String password, String name) {
        requestDTO.setEmail(email);
        requestDTO.setPassword(password);
        requestDTO.setName(name);

        Response firstResponse = given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();

        UserRs firstResponseBody = firstResponse.getBody().as(UserRs.class);

        accessToken = firstResponseBody.getAccessToken();
        refreshToken = firstResponseBody.getRefreshToken();

        assertTrue(firstResponseBody.isSuccess());
        assertEquals(email, firstResponseBody.getUser().getEmail());
        assertEquals(name, firstResponseBody.getUser().getName());
        assertFalse(firstResponseBody.getAccessToken().isEmpty());
        assertFalse(firstResponseBody.getRefreshToken().isEmpty());
    }

    @Step("Попытка повторной регистрации того же пользователя")
    private void attemptToRegisterUserAgain(UserRq requestDTO) {
        Response secondResponse = given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .extract()
                .response();

        assertEquals(403, secondResponse.getStatusCode());
        assertFalse(secondResponse.getBody().jsonPath().getBoolean("success"));
        assertEquals("User already exists", secondResponse.getBody().jsonPath().getString("message"));
    }

    @Test
    @Step("Тест создания пользователя с отсутствующим полем")
    public void testCreateUserMissingField() {
        UserRq requestDTO = new UserRq();
        requestDTO.setEmail("missing-field@example.com");
        requestDTO.setPassword("password");

        Response response = given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .extract()
                .response();

        assertEquals(403, response.getStatusCode());
        assertFalse(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("Email, password and name are required fields", response.getBody().jsonPath().getString("message"));
    }
}
