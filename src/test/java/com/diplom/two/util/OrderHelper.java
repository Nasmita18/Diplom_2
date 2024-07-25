package com.diplom.two.util;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class OrderHelper {

    @Step("Получение списка идентификаторов ингредиентов")
    public static List<String> getIngredientIds() {
        Response response = given()
                .when()
                .get("/api/ingredients")
                .then()
                .statusCode(200)
                .extract()
                .response();

        List<Map<String, String>> ingredients = response.jsonPath().getList("data");
        List<String> ingredientIds = new ArrayList<>();

        for (Map<String, String> ingredient : ingredients) {
            ingredientIds.add(ingredient.get("_id"));
        }

        return ingredientIds;
    }
}
