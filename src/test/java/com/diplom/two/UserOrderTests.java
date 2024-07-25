package com.diplom.two;

import com.diplom.two.dto.OrderRequest;
import com.diplom.two.dto.UserRq;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.diplom.two.util.OrderHelper.getIngredientIds;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserOrderTests {

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
    @Step("Получение заказов для авторизованного пользователя")
    public void testGetOrdersForAuthorizedUser() {
        List<String> ingredientIds = getIngredientIds();

        OrderRequest request = new OrderRequest();
        request.setIngredients(ingredientIds);

        given()
                .header("Authorization", accessToken)
                .contentType("application/json")
                .body(request)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(200);

        Response response = given()
                .header("Authorization", accessToken)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertTrue(response.jsonPath().getBoolean("success"));
        assertTrue(response.jsonPath().getList("orders").size() > 0);
    }

    @Test
    @Step("Получение заказов для неавторизованного пользователя")
    public void testGetOrdersForUnauthorizedUser() {
        Response response = given()
                .when()
                .get("/api/orders")
                .then()
                .statusCode(401)
                .extract()
                .response();

        assertEquals("You should be authorised", response.jsonPath().getString("message"));
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
