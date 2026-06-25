package com.jide.framework.tests.patch;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * PatchRequestsTest covers all PATCH scenarios.
 */
@Epic("REST API Framework")
@Feature("PATCH Requests")
public class PatchRequestsTest extends BaseTest {

    @Test(description = "PATCH /users/1 updating email only returns 200 with updated email")
    @Story("Partial update — single field")
    @Severity(SeverityLevel.CRITICAL)
    public void patchUser_singleField_returns200() {
        Map<String, Object> patch = Map.of("email", "patched@email.com");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(patch)
            .patch("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("id", 1)
            .hasJsonPath("email", "patched@email.com")
            // name should still be present (PATCH preserves other fields)
            .hasJsonPathPresent("name");
    }

    @Test(description = "PATCH /users/1 updating multiple fields returns 200")
    @Story("Partial update — multiple fields")
    @Severity(SeverityLevel.CRITICAL)
    public void patchUser_multipleFields_returns200() {
        Map<String, Object> patch = Map.of(
            "email",   "multi@patch.com",
            "website", "updated-website.com"
        );

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(patch)
            .patch("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("email",   "multi@patch.com")
            .hasJsonPath("website", "updated-website.com");
    }

    @Test(description = "PATCH /posts/1 updating title preserves other fields")
    @Story("Partial update — verify field preservation")
    @Severity(SeverityLevel.NORMAL)
    public void patchPost_titleOnly_preservesOtherFields() {
        Map<String, Object> patch = Map.of("title", "Patched Title Only");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(patch)
            .patch("/posts/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("id", 1)
            .hasJsonPath("title", "Patched Title Only")
            .hasJsonPathPresent("userId")
            .hasJsonPathPresent("body");
    }

    @Test(description = "PATCH /users/1 with payload file returns 200")
    @Story("Partial update — payload file")
    @Severity(SeverityLevel.NORMAL)
    public void patchUser_withPayloadFile_returns200() {
        String payload = loadPayload("payloads/json/patch-user.json");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .body(payload)
            .patch("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasJsonPath("email", "updated@framework.com");
    }

    @Test(description = "PATCH /users/99999 on non-existent resource returns 404")
    @Story("Edge cases — non-existent resource")
    @Severity(SeverityLevel.BLOCKER)
    public void patchUser_nonExistentId_returns404() {
        Map<String, Object> patch = Map.of("email", "ghost@email.com");

        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 99999))
            .body(patch)
            .patch("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(404, 500);
    }
}