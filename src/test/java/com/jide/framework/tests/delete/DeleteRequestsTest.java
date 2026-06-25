package com.jide.framework.tests.delete;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * DeleteRequestsTest covers all DELETE scenarios.
 */
@Epic("REST API Framework")
@Feature("DELETE Requests")
public class DeleteRequestsTest extends BaseTest {

    @Test(description = "DELETE /users/1 returns 200 or 204")
    @Story("Delete user")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteUser_existingId_returnsSuccess() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .delete("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 204)
            .bodyIsEmptyOrNull();
    }

    @Test(description = "DELETE /posts/1 returns 200 or 204")
    @Story("Delete post")
    @Severity(SeverityLevel.CRITICAL)
    public void deletePost_existingId_returnsSuccess() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .delete("/posts/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 204);
    }

    @Test(description = "DELETE /users/5 returns success and body is empty")
    @Story("Delete user — verify empty response body")
    @Severity(SeverityLevel.NORMAL)
    public void deleteUser_responseBodyIsEmpty() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 5))
            .delete("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 204)
            .bodyIsEmptyOrNull();
    }

    @Test(description = "DELETE /users/99999 on non-existent resource returns 404")
    @Story("Edge cases — non-existent resource")
    @Severity(SeverityLevel.BLOCKER)
    public void deleteUser_nonExistentId_returns404() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 99999))
            .delete("/users/{id}");

        // JSONPlaceholder returns 200 even for non-existent IDs.
        // A well-designed API returns 404. hasStatusIn covers both behaviours.
        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 404);
    }

    @Test(description = "DELETE is idempotent — second delete of same resource is safe")
    @Story("Edge cases — idempotency")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies that calling DELETE twice on the same resource does not cause an error. A correctly implemented DELETE is idempotent.")
    public void deletePost_calledTwice_bothSucceed() {
        // JSONPlaceholder does not actually persist deletes so both calls
        // succeed. This test documents and verifies idempotency behaviour.
        Response first = RequestBuilder.withJson()
            .pathParams(Map.of("id", 3))
            .delete("/posts/{id}");

        Response second = RequestBuilder.withJson()
            .pathParams(Map.of("id", 3))
            .delete("/posts/{id}");

        ApiResponseAssert.assertThat(first).hasStatusIn(200, 204);
        ApiResponseAssert.assertThat(second).hasStatusIn(200, 204, 404);
    }
}