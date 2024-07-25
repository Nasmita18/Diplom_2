package com.diplom.two;

import com.diplom.two.dto.OrderRequest;
import com.diplom.two.dto.UserRq;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.diplom.two.util.OrderHelper.getIngredientIds;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class OrderTests {

    private String accessToken;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
        accessToken = registerUser("test" + System.currentTimeMillis() + "@example.com", "password", "testuser");
    }

    @After
    public void teardown() {
        if (accessToken != null) {
            deleteUser();
        }
    }

    @Test
    @Step("Создание заказа с авторизацией")
    public void testCreateOrderWithAuthorization() {
        List<String> ingredientIds = getIngredientIds();

        OrderRequest request = new OrderRequest();
        request.setIngredients(ingredientIds);

        Response response = given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertTrue(response.jsonPath().getBoolean("success"));
        assertNotNull(response.jsonPath().getString("order._id")); // Проверка наличия поля _id внутри order
    }

    @Test
    @Step("Создание заказа без авторизации")
    public void testCreateOrderWithoutAuthorization() {
        List<String> ingredientIds = getIngredientIds();

        OrderRequest request = new OrderRequest();
        request.setIngredients(ingredientIds);

        Response response = given()
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertNull(response.jsonPath().getString("order._id")); // Проверка наличия поля _id внутри order

    }

    @Test
    @Step("Создание заказа без ингредиентов")
    public void testCreateOrderWithoutIngredients() {
        OrderRequest request = new OrderRequest();
        request.setIngredients(new ArrayList<>());

        Response response = given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(400)
                .extract()
                .response();

        assertEquals("Ingredient ids must be provided", response.jsonPath().getString("message"));
    }

    @Test
    @Step("Создание заказа с неверным хешем ингредиентов")
    public void testCreateOrderWithInvalidIngredientHash() {
        OrderRequest request = new OrderRequest();
        request.setIngredients(List.of("invalid ыhash"));

        Response response = given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(500)
                .extract()
                .response();

        assertEquals(500, response.getStatusCode());
    }

    @Step("Регистрация пользователя с email: {email}, паролем: {password}, именем: {name}")
    private String registerUser(String email, String password, String name) {
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
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("accessToken");
    }

    @Step("Удаление пользователя с токеном доступа")
    private void deleteUser() {
        given()
                .header("Authorization", accessToken)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }
}
