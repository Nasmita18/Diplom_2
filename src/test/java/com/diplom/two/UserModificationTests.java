package com.diplom.two;

import com.diplom.two.dto.UserRq;
import com.diplom.two.dto.UserRs;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.Step;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserModificationTests {

    private String accessToken;
    private String refreshToken;
    private String existingEmail = "existing-user@example.com";
    private String existingPassword = "password";
    private String nonExistingEmail = "nonexisting-user@example.com";
    private String wrongPassword = "wrongpassword";

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

    @Step("Регистрация пользователя с email: {0}, паролем: {1} и именем: {2}")
    private void registerUser(String email, String password, String name) {
        UserRq requestDTO = new UserRq();
        requestDTO.setEmail(email);
        requestDTO.setPassword(password);
        requestDTO.setName(name);

        Response response = given()
                .contentType("application/json")
                .body(requestDTO)
                .when()
                .post("/api/auth/register")
                .then()
                .extract()
                .response();

        // Получение данных из ответа с помощью JsonPath
        accessToken = response.jsonPath().getString("accessToken");
        refreshToken = response.jsonPath().getString("refreshToken");
    }

    @Test
    @Step("Логин под существующим пользователем")
    public void testLoginUserSuccess() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        UserRq loginRequest = new UserRq();
        loginRequest.setEmail(existingEmail);
        loginRequest.setPassword(existingPassword);

        Response response = given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();

        UserRs responseBody = response.getBody().as(UserRs.class);
        accessToken = responseBody.getAccessToken();
        refreshToken = responseBody.getRefreshToken();

        assertEquals(200, response.getStatusCode());
        assertTrue(responseBody.isSuccess());
        assertEquals(existingEmail, responseBody.getUser().getEmail());
        assertEquals("ExistingUser", responseBody.getUser().getName());
        assertFalse(responseBody.getAccessToken().isEmpty());
        assertFalse(responseBody.getRefreshToken().isEmpty());
    }

    @Test
    @Step("Логин с неверным логином и паролем")
    public void testLoginUserWrongCredentials() {
        UserRq loginRequest = new UserRq();
        loginRequest.setEmail(nonExistingEmail);
        loginRequest.setPassword(wrongPassword);

        Response response = given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();

        assertEquals(401, response.getStatusCode());
        assertFalse(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("email or password are incorrect", response.getBody().jsonPath().getString("message"));
    }

    @Test
    @Step("Успешная регистрация пользователя")
    public void testRegisterUserSuccess() {
        String uniqueEmail = "test-user-" + System.currentTimeMillis() + "@example.com";
        UserRq requestDTO = new UserRq();
        requestDTO.setEmail(uniqueEmail);
        requestDTO.setPassword("password123");
        requestDTO.setName("TestUser");

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
        assertEquals("TestUser", responseBody.getUser().getName());
        assertFalse(responseBody.getAccessToken().isEmpty());
        assertFalse(responseBody.getRefreshToken().isEmpty());
    }

    @Test
    @Step("Успешный выход из системы")
    public void testLogoutUserSuccess() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        UserRq loginRequest = new UserRq();
        loginRequest.setEmail(existingEmail);
        loginRequest.setPassword(existingPassword);

        Response loginResponse = given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();

        UserRs loginResponseBody = loginResponse.getBody().as(UserRs.class);
        accessToken = loginResponseBody.getAccessToken();
        refreshToken = loginResponseBody.getRefreshToken();

        Response logoutResponse = given()
                .contentType("application/json")
                .body("{\"token\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/auth/logout")
                .then()
                .extract()
                .response();

        assertEquals(200, logoutResponse.getStatusCode());
        assertTrue(logoutResponse.getBody().jsonPath().getBoolean("success"));
        assertEquals("Successful logout", logoutResponse.getBody().jsonPath().getString("message"));
    }

    @Test
    @Step("Обновление токена")
    public void testTokenRefresh() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        UserRq loginRequest = new UserRq();
        loginRequest.setEmail(existingEmail);
        loginRequest.setPassword(existingPassword);

        Response loginResponse = given()
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .extract()
                .response();

        UserRs loginResponseBody = loginResponse.getBody().as(UserRs.class);
        accessToken = loginResponseBody.getAccessToken();
        refreshToken = loginResponseBody.getRefreshToken();

        Response tokenResponse = given()
                .contentType("application/json")
                .body("{\"token\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/auth/token")
                .then()
                .extract()
                .response();

        assertEquals(200, tokenResponse.getStatusCode());
        assertTrue(tokenResponse.getBody().jsonPath().getBoolean("success"));
        assertFalse(tokenResponse.getBody().jsonPath().getString("accessToken").isEmpty());
    }

    @Test
    @Step("Получение информации о пользователе")
    public void testGetUserInfo() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        Response response = given()
                .header("Authorization", accessToken)
                .when()
                .get("/api/auth/user")
                .then()
                .extract()
                .response();

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().jsonPath().getBoolean("success"));
        assertEquals(existingEmail, response.getBody().jsonPath().getString("user.email"));
        assertEquals("ExistingUser", response.getBody().jsonPath().getString("user.name"));
    }

    @Test
    @Step("Изменение данных пользователя с авторизацией")
    public void testUpdateUserInfoWithAuth() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        UserRq updateRequest = new UserRq();
        updateRequest.setEmail("updated-" + existingEmail);
        updateRequest.setName("UpdatedUser");

        Response response = given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(updateRequest)
                .when()
                .patch("/api/auth/user")
                .then()
                .extract()
                .response();

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("updated-" + existingEmail, response.getBody().jsonPath().getString("user.email"));
        assertEquals("UpdatedUser", response.getBody().jsonPath().getString("user.name"));
    }

    @Test
    @Step("Изменение данных пользователя без авторизации")
    public void testUpdateUserInfoWithoutAuth() {
        UserRq updateRequest = new UserRq();
        updateRequest.setEmail("updated-" + existingEmail);
        updateRequest.setName("UpdatedUser");

        Response response = given()
                .contentType("application/json")
                .body(updateRequest)
                .when()
                .patch("/api/auth/user")
                .then()
                .extract()
                .response();

        assertEquals(401, response.getStatusCode());
        assertFalse(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("You should be authorised", response.getBody().jsonPath().getString("message"));
    }

    @Test
    @Step("Попытка обновления email на уже существующий")
    public void testUpdateUserEmailToExisting() {
        registerUser(existingEmail, existingPassword, "ExistingUser");

        String uniqueEmail = "unique-user-" + System.currentTimeMillis() + "@example.com";
        registerUser(uniqueEmail, existingPassword, "UniqueUser");

        UserRq updateRequest = new UserRq();
        updateRequest.setEmail(existingEmail);
        updateRequest.setName("UniqueUser");

        Response response = given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(updateRequest)
                .when()
                .patch("/api/auth/user")
                .then()
                .extract()
                .response();

        assertEquals(403, response.getStatusCode());
        assertFalse(response.getBody().jsonPath().getBoolean("success"));
        assertEquals("User with such email already exists", response.getBody().jsonPath().getString("message"));
    }
}
