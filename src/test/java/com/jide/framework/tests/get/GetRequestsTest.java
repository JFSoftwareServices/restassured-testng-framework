package com.jide.framework.tests.get;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * GetRequestsTest covers all GET scenarios including:
 * - Retrieve a collection (list of resources)
 * - Retrieve a single resource by ID
 * - Retrieve with query parameters (filtering, pagination)
 * - Nested resource retrieval
 * - Edge cases: non-existent ID (404), invalid ID format
 * - JSON Schema validation on collection and single resource
 * - Response time assertion
 * <p>
 * All tests use the /users and /posts endpoints of JSONPlaceholder.
 */
@Epic("REST API Framework")
@Feature("GET Requests")
public class GetRequestsTest extends BaseTest {

    // -------------------------------------------------------------------------
    // Collection retrieval
    // -------------------------------------------------------------------------

    @Test(description = "GET /users returns a list with 10 users")
    @Story("Retrieve all users")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that GET /users returns HTTP 200, a JSON Content-Type header, exactly 10 users, and responds within the SLA.")
    public void getAllUsers_returns200WithList() {
        Response response = RequestBuilder.withJson().get("/users");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasHeader("Content-Type", containsString("application/json"))
                .hasJsonPath("$.size()", 10)
                .respondsWithinMs(5000);
    }

    @Test(description = "GET /users collection matches JSON schema")
    @Story("Retrieve all users")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Validates the full /users response body against the user-list JSON Schema to detect structural API breaking changes.")
    public void getAllUsers_matchesJsonSchema() {
        Response response = RequestBuilder.withJson().get("/users");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .matchesJsonSchema("schemas/json/user-list-schema.json");
    }

    @Test(description = "GET /posts returns a list with 100 posts")
    @Story("Retrieve all posts")
    @Severity(SeverityLevel.NORMAL)
    public void getAllPosts_returns200WithList() {
        Response response = RequestBuilder.withJson().get("/posts");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("$.size()", 100);
    }

    // -------------------------------------------------------------------------
    // Single resource by ID
    // -------------------------------------------------------------------------

    @Test(description = "GET /users/1 returns the correct user")
    @Story("Retrieve user by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that a single user resource is returned with the correct id, non-null name and email, and passes schema validation.")
    public void getUserById_returns200WithCorrectUser() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("id", 1))
                .get("/users/{id}");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("id", 1)
                .hasJsonPath("name", notNullValue())
                .hasJsonPath("email", notNullValue())
                .matchesJsonSchema("schemas/json/user-schema.json");
    }

    @Test(description = "GET /posts/1 returns the correct post")
    @Story("Retrieve post by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that a single post resource is returned with the correct id and userId, and passes schema validation.")
    public void getPostById_returns200WithCorrectPost() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("id", 1))
                .get("/posts/{id}");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("id", 1)
                .hasJsonPath("userId", 1)
                .hasJsonPath("title", notNullValue())
                .matchesJsonSchema("schemas/json/post-schema.json");
    }

    // -------------------------------------------------------------------------
    // Query parameters
    // -------------------------------------------------------------------------

    @Test(description = "GET /posts?userId=1 filters posts by userId")
    @Story("Filter posts by query parameter")
    @Severity(SeverityLevel.NORMAL)
    public void getPosts_withQueryParam_userId_filtersCorrectly() {
        Response response = RequestBuilder.withJson()
                .queryParams(Map.of("userId", 1))
                .get("/posts");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200);

        List<Integer> userIds = response.jsonPath().getList("userId");
        userIds.forEach(id ->
                org.hamcrest.MatcherAssert.assertThat(
                        "All posts must have userId 1", id, equalTo(1))
        );
    }

    @Test(description = "GET /posts?userId=1&_limit=3 returns at most 3 results")
    @Story("Filter posts by query parameter")
    @Severity(SeverityLevel.NORMAL)
    public void getPosts_withMultipleQueryParams_limitsResults() {
        Response response = RequestBuilder.withJson()
                .queryParams(Map.of("userId", 1, "_limit", 3))
                .get("/posts");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("$.size()", 3);
    }

    // -------------------------------------------------------------------------
    // Nested resources
    // -------------------------------------------------------------------------

    @Test(description = "GET /users/1/posts returns posts for user 1")
    @Story("Retrieve nested resources")
    @Severity(SeverityLevel.NORMAL)
    public void getPostsForUser_returns200WithNonEmptyList() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("userId", 1))
                .get("/users/{userId}/posts");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("$.size()", greaterThan(0));
    }

    @Test(description = "GET /posts/1/comments returns comments for post 1")
    @Story("Retrieve nested resources")
    @Severity(SeverityLevel.NORMAL)
    public void getCommentsForPost_returns200() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("postId", 1))
                .get("/posts/{postId}/comments");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .hasJsonPath("$.size()", greaterThan(0));
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test(description = "GET /users/99999 returns 404 for non-existent resource")
    @Story("Handle non-existent resources")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Edge case: requesting a user ID that does not exist must return 404. A 200 with empty body would be an API defect.")
    public void getUserById_nonExistentId_returns404() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("id", 99999))
                .get("/users/{id}");

        ApiResponseAssert.assertThat(response)
                .hasStatus(404);
    }

    @Test(description = "GET /posts/99999 returns 404 for non-existent post")
    @Story("Handle non-existent resources")
    @Severity(SeverityLevel.BLOCKER)
    public void getPostById_nonExistentId_returns404() {
        Response response = RequestBuilder.withJson()
                .pathParams(Map.of("id", 99999))
                .get("/posts/{id}");

        ApiResponseAssert.assertThat(response)
                .hasStatus(404);
    }

    @Test(description = "Response time for GET /users is within acceptable threshold")
    @Story("Performance — response time SLA")
    @Severity(SeverityLevel.MINOR)
    public void getAllUsers_respondsWithinSla() {
        Response response = RequestBuilder.withJson().get("/users");

        ApiResponseAssert.assertThat(response)
                .hasStatus(200)
                .respondsWithinMs(3000);
    }
}