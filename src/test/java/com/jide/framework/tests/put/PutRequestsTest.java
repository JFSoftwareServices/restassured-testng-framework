package com.jide.framework.tests.put;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.models.User;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * PutRequestsTest covers all PUT scenarios.
 */
@Epic("REST API Framework")
@Feature("PUT Requests")
public class PutRequestsTest extends BaseTest {

    @Test(description = "PUT /users/1 with POJO replaces and returns 200 with updated fields")
    @Story("Replace user resource")
    @Severity(SeverityLevel.CRITICAL)
    public void updateUser_withPojo_returns200() {
        User updated = new User("Updated Name", "updated.user", "updated@email.com");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(updated)
            .put("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("id", 1)
            .hasJsonPath("name", "Updated Name")
            .hasJsonPath("username", "updated.user")
            .hasJsonPath("email", "updated@email.com");
    }

    @Test(description = "PUT /posts/1 with Map replaces and returns 200")
    @Story("Replace post resource")
    @Severity(SeverityLevel.CRITICAL)
    public void updatePost_withMap_returns200() {
        Map<String, Object> body = Map.of(
            "userId", 1,
            "title",  "Replaced Post Title",
            "body",   "Completely replaced post body content"
        );

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(body)
            .put("/posts/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("id", 1)
            .hasJsonPath("title", "Replaced Post Title")
            .hasJsonPath("userId", 1);
    }

    @Test(description = "PUT /users/1 with raw JSON payload file returns 200")
    @Story("Replace user resource")
    @Severity(SeverityLevel.NORMAL)
    public void updateUser_withPayloadFile_returns200() {
        String payload = loadPayload("payloads/json/create-user.json");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(payload)
            .put("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("name", "Jide Framework User")
            .hasJsonPath("email", "jide@framework.com");
    }

    @Test(description = "PUT /users/1 response matches user JSON schema")
    @Story("Schema validation")
    @Severity(SeverityLevel.CRITICAL)
    public void updateUser_responseMatchesSchema() {
        User updated = new User("Schema Test User", "schema.user", "schema@test.com");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(updated)
            .put("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .matchesJsonSchema("schemas/json/user-schema.json");
    }

    @Test(description = "PUT to non-existent resource returns 404 or 500")
    @Story("Edge cases — non-existent resource")
    @Severity(SeverityLevel.BLOCKER)
    public void updateUser_nonExistentId_returnsErrorStatus() {
        User updated = new User("Ghost User", "ghost", "ghost@nowhere.com");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 99999))
            .body(updated)
            .put("/users/{id}");

        // JSONPlaceholder returns 500 for non-existent PUT; a well-designed
        // REST API would return 404. Either is asserted here to cover both.
        ApiResponseAssert.assertThat(response)
            .hasStatusIn(404, 500);
    }
}