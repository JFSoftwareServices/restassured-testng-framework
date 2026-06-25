package com.jide.framework.client;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.qameta.allure.Step;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * RequestBuilder provides a fluent API for constructing and sending HTTP
 * requests, wrapping RestAssured's given/when/then chain.
 *
 * Each method returns the Response directly, leaving assertion to
 * ApiResponseAssert. This separation of concerns keeps test code clean:
 *
 *   Response response = RequestBuilder.withJson()
 *       .pathParam("id", 1)
 *       .get("/users/{id}");
 *
 *   ApiResponseAssert.assertThat(response)
 *       .hasStatus(200)
 *       .hasJsonPath("name", "Leanne Graham");
 *
 * A new RequestBuilder is created per call — it is stateless and not shared
 * between threads. The underlying RequestSpecification it reads from ApiClient
 * is ThreadLocal, so thread safety is guaranteed.
 *
 * Each HTTP method is annotated with @Step so that Allure records it as a
 * named step in the report (e.g. "GET /users/1"), making the report readable
 * without needing to inspect raw request logs. @Step requires the AspectJ
 * weaver to be on the JVM javaagent (configured in pom.xml Surefire argLine).
 */
public class RequestBuilder {

    private final RequestSpecification spec;
    private Map<String, Object> pathParams;
    private Map<String, Object> queryParams;
    private Map<String, String> headers;
    private Object body;

    private RequestBuilder(RequestSpecification spec) {
        this.spec = spec;
    }

    // -------------------------------------------------------------------------
    // Entry points
    // -------------------------------------------------------------------------

    /** Returns a builder pre-configured with the JSON spec for this thread. */
    public static RequestBuilder withJson() {
        return new RequestBuilder(ApiClient.jsonSpec());
    }

    /** Returns a builder pre-configured with the XML spec for this thread. */
    public static RequestBuilder withXml() {
        return new RequestBuilder(ApiClient.xmlSpec());
    }

    // -------------------------------------------------------------------------
    // Request configuration
    // -------------------------------------------------------------------------

    public RequestBuilder pathParams(Map<String, Object> params) {
        this.pathParams = params;
        return this;
    }

    public RequestBuilder queryParams(Map<String, Object> params) {
        this.queryParams = params;
        return this;
    }

    public RequestBuilder headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public RequestBuilder body(Object body) {
        this.body = body;
        return this;
    }

    // -------------------------------------------------------------------------
    // HTTP method execution
    // -------------------------------------------------------------------------

    @Step("GET {path}")
    public Response get(String path) {
        return build().get(path);
    }

    @Step("POST {path}")
    public Response post(String path) {
        return build().post(path);
    }

    @Step("PUT {path}")
    public Response put(String path) {
        return build().put(path);
    }

    @Step("PATCH {path}")
    public Response patch(String path) {
        return build().patch(path);
    }

    @Step("DELETE {path}")
    public Response delete(String path) {
        return build().delete(path);
    }

    @Step("HEAD {path}")
    public Response head(String path) {
        return build().head(path);
    }

    @Step("OPTIONS {path}")
    public Response options(String path) {
        return build().options(path);
    }

    // -------------------------------------------------------------------------
    // Internal builder
    // -------------------------------------------------------------------------

    private RequestSpecification build() {
        RequestSpecification req = given().spec(spec);

        if (pathParams  != null) req = req.pathParams(pathParams);
        if (queryParams != null) req = req.queryParams(queryParams);
        if (headers     != null) req = req.headers(headers);
        if (body        != null) req = req.body(body);

        return req;
    }
}