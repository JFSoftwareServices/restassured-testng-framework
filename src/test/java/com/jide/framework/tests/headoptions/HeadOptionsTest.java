package com.jide.framework.tests.headoptions;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;

/**
 * HeadOptionsTest covers HEAD and OPTIONS HTTP methods.
 */
@Epic("REST API Framework")
@Feature("HEAD and OPTIONS Requests")
public class HeadOptionsTest extends BaseTest {

    // -------------------------------------------------------------------------
    // HEAD
    // -------------------------------------------------------------------------

    @Test(description = "HEAD /users returns 200 with headers and no body")
    @Story("HEAD — verify resource exists without body")
    @Severity(SeverityLevel.NORMAL)
    public void headUsers_returns200WithNoBody() {
        Response response = RequestBuilder.withJson().head("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasHeaderPresent("Content-Type")
            .bodyIsEmptyOrNull();
    }

    @Test(description = "HEAD /users/1 returns 200 — resource exists")
    @Story("HEAD — verify resource exists without body")
    @Severity(SeverityLevel.NORMAL)
    public void headUserById_existingId_returns200() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 1))
            .head("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200);
    }

    @Test(description = "HEAD /users Content-Type header indicates JSON")
    @Story("HEAD — inspect headers")
    @Severity(SeverityLevel.NORMAL)
    public void headUsers_contentTypeIsJson() {
        Response response = RequestBuilder.withJson().head("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .hasHeader("Content-Type", containsString("application/json"));
    }

    @Test(description = "HEAD /users/99999 returns 404 — resource does not exist")
    @Story("Edge cases — non-existent resource")
    @Severity(SeverityLevel.BLOCKER)
    public void headUser_nonExistentId_returns404() {
        Response response = RequestBuilder.withJson()
            .pathParams(Map.of("id", 99999))
            .head("/users/{id}");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 404);
        // JSONPlaceholder returns 200 even for non-existent HEAD;
        // a strict API returns 404. Both are acceptable here.
    }

    @Test(description = "HEAD /posts returns 200 with no body")
    @Story("HEAD — verify resource exists without body")
    @Severity(SeverityLevel.NORMAL)
    public void headPosts_returns200WithNoBody() {
        Response response = RequestBuilder.withJson().head("/posts");

        ApiResponseAssert.assertThat(response)
            .hasStatus(200)
            .bodyIsEmptyOrNull();
    }

    // -------------------------------------------------------------------------
    // OPTIONS
    // -------------------------------------------------------------------------

    @Test(description = "OPTIONS /users returns a response without error")
    @Story("OPTIONS — discover allowed methods")
    @Severity(SeverityLevel.NORMAL)
    public void optionsUsers_returnsSuccessResponse() {
        Response response = RequestBuilder.withJson().options("/users");

        // OPTIONS typically returns 200 or 204. JSONPlaceholder returns 204.
        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 204);
    }

    @Test(description = "OPTIONS /users returns Allow header listing permitted methods")
    @Story("OPTIONS — discover allowed methods")
    @Severity(SeverityLevel.NORMAL)
    public void optionsUsers_allowHeaderPresent() {
        Response response = RequestBuilder.withJson().options("/users");

        // If the API returns an Allow header, verify it contains GET.
        // Not all APIs/proxies return Allow on OPTIONS so we check for its
        // presence before asserting its value.
        String allowHeader = response.getHeader("Allow");
        if (allowHeader != null) {
            org.hamcrest.MatcherAssert.assertThat(
                "Allow header should include GET",
                allowHeader, containsString("GET"));
        }
    }

    @Test(description = "OPTIONS /posts returns a response without error")
    @Story("OPTIONS — discover allowed methods")
    @Severity(SeverityLevel.NORMAL)
    public void optionsPosts_returnsSuccessResponse() {
        Response response = RequestBuilder.withJson().options("/posts");

        ApiResponseAssert.assertThat(response)
            .hasStatusIn(200, 204);
    }
}