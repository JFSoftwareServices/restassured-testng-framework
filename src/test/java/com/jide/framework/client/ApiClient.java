package com.jide.framework.client;

import com.jide.framework.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * ApiClient provides thread-safe access to RestAssured RequestSpecification
 * instances for both JSON and XML content types.
 *
 * <h3>Why ThreadLocal</h3>
 *
 * RestAssured's RequestSpecification is a mutable object. If a single shared
 * instance were used across all threads, one thread could mutate it (adding
 * an Authorization header after login, for example) while another thread is
 * in the middle of building a request from it. This produces race conditions
 * that manifest as intermittent test failures that are extremely difficult to
 * reproduce and diagnose.
 *
 * ThreadLocal solves this by giving each thread its own independent instance.
 * When a thread calls spec(), it always gets the instance it owns — no other
 * thread can read or write it.
 *
 * <h3>Performance</h3>
 *
 * ThreadLocal.withInitial() runs the factory lambda only once per thread,
 * the first time that thread calls get(). Every subsequent call on that
 * thread returns the cached instance. With 8 parallel threads each running
 * many test methods, the spec is built 8 times rather than once per test.
 *
 * <h3>Resetting between tests</h3>
 *
 * TestNG reuses threads across test methods. A spec mutated during one test
 * (e.g. an auth header added) will still carry that mutation when the next
 * test runs on the same thread. Call ApiClient.reset() in @BeforeMethod
 * (done in BaseTest) to discard the cached spec. The next call to spec()
 * rebuilds it fresh via the factory lambda.
 *
 * <h3>Two specs: JSON and XML</h3>
 *
 * Separate ThreadLocal instances are maintained for JSON and XML because
 * the Content-Type and Accept headers differ. Tests use jsonSpec() or
 * xmlSpec() depending on the payload format being tested.
 */
public class ApiClient {

    private static final ThreadLocal<RequestSpecification> JSON_SPEC =
        ThreadLocal.withInitial(ApiClient::buildJsonSpec);

    private static final ThreadLocal<RequestSpecification> XML_SPEC =
        ThreadLocal.withInitial(ApiClient::buildXmlSpec);

    private ApiClient() {}

    // -------------------------------------------------------------------------
    // Spec accessors
    // -------------------------------------------------------------------------

    /** Returns this thread's JSON RequestSpecification. Creates it on first call. */
    public static RequestSpecification jsonSpec() {
        return JSON_SPEC.get();
    }

    /** Returns this thread's XML RequestSpecification. Creates it on first call. */
    public static RequestSpecification xmlSpec() {
        return XML_SPEC.get();
    }

    // -------------------------------------------------------------------------
    // Header mutation (progressive building for auth flows etc.)
    // -------------------------------------------------------------------------

    /** Adds or overwrites a header on this thread's JSON spec. */
    public static void addHeader(String name, String value) {
        JSON_SPEC.get().header(name, value);
    }

    /** Adds or overwrites a header on this thread's XML spec. */
    public static void addXmlHeader(String name, String value) {
        XML_SPEC.get().header(name, value);
    }

    // -------------------------------------------------------------------------
    // Reset — call in @BeforeMethod to prevent stale state bleed
    // -------------------------------------------------------------------------

    /**
     * Discards this thread's cached JSON and XML specs.
     * The next call to jsonSpec() or xmlSpec() rebuilds them fresh.
     * Must be called in @BeforeMethod to prevent header/state leakage
     * between parallel test methods running on the same thread.
     */
    public static void reset() {
        JSON_SPEC.remove();
        XML_SPEC.remove();
    }

    // -------------------------------------------------------------------------
    // Spec builders — private, called once per thread by withInitial()
    // -------------------------------------------------------------------------

    private static RequestSpecification buildJsonSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setBaseUri(ConfigManager.getJsonBaseUrl())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(buildRestAssuredConfig())
            // AllureRestAssured automatically attaches the full HTTP request
            // and response as attachments in the Allure report for every test.
            // No additional code is needed in test classes — adding it here
            // means every request made through this spec is captured.
            .addFilter(new AllureRestAssured());

        if (ConfigManager.getBoolean("log.requests", true)) {
            builder.addFilter(new RequestLoggingFilter());
        }
        if (ConfigManager.getBoolean("log.responses", true)) {
            builder.addFilter(new ResponseLoggingFilter());
        }

        return builder.build();
    }

    private static RequestSpecification buildXmlSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setBaseUri(ConfigManager.getXmlBaseUrl())
            .setContentType(ContentType.XML)
            .setAccept(ContentType.XML)
            .setConfig(buildRestAssuredConfig())
            .addFilter(new AllureRestAssured());

        if (ConfigManager.getBoolean("log.requests", true)) {
            builder.addFilter(new RequestLoggingFilter());
        }
        if (ConfigManager.getBoolean("log.responses", true)) {
            builder.addFilter(new ResponseLoggingFilter());
        }

        return builder.build();
    }

    private static RestAssuredConfig buildRestAssuredConfig() {
        int connectTimeout = ConfigManager.getInt("connection.timeout", 5000);
        int readTimeout    = ConfigManager.getInt("read.timeout", 10000);

        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", connectTimeout)
                .setParam("http.socket.timeout", readTimeout));
    }
}