package com.jide.framework.assertions;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

/**
 * ApiResponseAssert is a fluent assertion DSL for HTTP responses.
 *
 * It wraps RestAssured's response validation and Hamcrest matchers so that
 * test assertion chains read like a plain English specification:
 *
 *   ApiResponseAssert.assertThat(response)
 *       .hasStatus(200)
 *       .hasHeader("Content-Type", containsString("application/json"))
 *       .hasJsonPath("id", 1)
 *       .matchesJsonSchema("schemas/json/user-schema.json");
 *
 * Every method returns `this`, enabling chaining. Assertions are executed
 * immediately on each call — there is no deferred execution.
 *
 * Hamcrest matchers are used throughout rather than plain equality checks
 * because they produce descriptive failure messages. For example:
 *
 *   assertThat("status code", actual, equalTo(200))
 *
 * produces on failure:
 *   Expected: <200> but was: <404>
 *
 * This is significantly more useful than a bare assertEquals failure.
 */
public class ApiResponseAssert {

    private final Response response;

    private ApiResponseAssert(Response response) {
        this.response = response;
    }

    public static ApiResponseAssert assertThat(Response response) {
        return new ApiResponseAssert(response);
    }

    // -------------------------------------------------------------------------
    // Status code
    // -------------------------------------------------------------------------

    public ApiResponseAssert hasStatus(int expectedStatus) {
        MatcherAssert.assertThat("HTTP status code",
            response.getStatusCode(), equalTo(expectedStatus));
        return this;
    }

    public ApiResponseAssert hasStatusIn(int... expectedStatuses) {
        int actual = response.getStatusCode();
        boolean matched = false;
        for (int s : expectedStatuses) {
            if (actual == s) { matched = true; break; }
        }
        MatcherAssert.assertThat("HTTP status code " + actual + " not in expected set",
            matched, is(true));
        return this;
    }

    // -------------------------------------------------------------------------
    // Headers
    // -------------------------------------------------------------------------

    public ApiResponseAssert hasHeader(String name, String expectedValue) {
        MatcherAssert.assertThat("Header '" + name + "'",
            response.getHeader(name), equalTo(expectedValue));
        return this;
    }

    public ApiResponseAssert hasHeader(String name, Matcher<String> matcher) {
        MatcherAssert.assertThat("Header '" + name + "'",
            response.getHeader(name), matcher);
        return this;
    }

    public ApiResponseAssert hasHeaderPresent(String name) {
        MatcherAssert.assertThat("Header '" + name + "' should be present",
            response.getHeader(name), notNullValue());
        return this;
    }

    // -------------------------------------------------------------------------
    // JSON body assertions
    // -------------------------------------------------------------------------

    public ApiResponseAssert hasJsonPath(String path, Object expectedValue) {
        MatcherAssert.assertThat("JSON path '" + path + "'",
            response.jsonPath().get(path), equalTo(expectedValue));
        return this;
    }

    public <T> ApiResponseAssert hasJsonPath(String path, Matcher<T> matcher) {
        MatcherAssert.assertThat("JSON path '" + path + "'",
            response.jsonPath().<T>get(path), matcher);
        return this;
    }

    public ApiResponseAssert hasJsonPathPresent(String path) {
        MatcherAssert.assertThat("JSON path '" + path + "' should be present",
            response.jsonPath().get(path), notNullValue());
        return this;
    }

    public ApiResponseAssert bodyIsEmptyOrNull() {
        String body = response.getBody().asString();
        MatcherAssert.assertThat("Body should be empty or null",
            body == null || body.trim().isEmpty() || body.trim().equals("{}") || body.trim().equals("null"),
            is(true));
        return this;
    }

    // -------------------------------------------------------------------------
    // XML body assertions
    // -------------------------------------------------------------------------

    public ApiResponseAssert hasXmlPath(String path, String expectedValue) {
        MatcherAssert.assertThat("XML path '" + path + "'",
            response.xmlPath().getString(path), equalTo(expectedValue));
        return this;
    }

    public ApiResponseAssert hasXmlPath(String path, Matcher<String> matcher) {
        MatcherAssert.assertThat("XML path '" + path + "'",
            response.xmlPath().getString(path), matcher);
        return this;
    }

    // -------------------------------------------------------------------------
    // Schema validation
    // -------------------------------------------------------------------------

    /**
     * Validates the response body against a JSON Schema file on the classpath.
     *
     * @param classpathPath path relative to the classpath root,
     *                      e.g. "schemas/json/user-schema.json"
     */
    public ApiResponseAssert matchesJsonSchema(String classpathPath) {
        response.then().body(matchesJsonSchemaInClasspath(classpathPath));
        return this;
    }

    /**
     * Validates the response body against an XSD file on the classpath.
     * Delegates to XmlSchemaValidator to keep this class focused on assertions.
     *
     * @param classpathPath path relative to the classpath root,
     *                      e.g. "schemas/xml/user-schema.xsd"
     */
    public ApiResponseAssert matchesXmlSchema(String classpathPath) {
        com.jide.framework.validators.XmlSchemaValidator
            .validate(response.getBody().asString(), classpathPath);
        return this;
    }

    // -------------------------------------------------------------------------
    // Response time
    // -------------------------------------------------------------------------

    public ApiResponseAssert respondsWithinMs(long maxMilliseconds) {
        MatcherAssert.assertThat("Response time should be under " + maxMilliseconds + "ms",
            response.getTime(), lessThan(maxMilliseconds));
        return this;
    }

    // -------------------------------------------------------------------------
    // Raw response accessor (for extracting values mid-chain)
    // -------------------------------------------------------------------------

    public Response andReturn() {
        return response;
    }
}