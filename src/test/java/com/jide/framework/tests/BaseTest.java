package com.jide.framework.tests;

import com.jide.framework.client.ApiClient;
import com.jide.framework.config.ConfigManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * BaseTest is the parent class for all test classes in the framework.
 *
 * It provides:
 *
 * 1. Suite-level setup (@BeforeSuite): logs the base URL being tested so
 *    the target environment is clearly visible in the console output at the
 *    start of every run. Runs once per suite, not per method.
 *
 * 2. Method-level reset (@BeforeMethod): calls ApiClient.reset() before
 *    every test method. This discards any mutated ThreadLocal
 *    RequestSpecification (e.g. one that had an Authorization header added
 *    during a previous test on the same thread) and forces a fresh spec to
 *    be built on the next call to ApiClient.jsonSpec() or xmlSpec().
 *
 *    This is non-negotiable in a parallel framework. TestNG reuses threads
 *    across test methods — without the reset, a header or cookie added in
 *    test method A on thread-3 will still be present when test method B runs
 *    on thread-3 later. The resulting false passes or false failures are some
 *    of the hardest bugs to diagnose because they only appear in parallel runs
 *    and depend on execution order.
 *
 * 3. Utility methods shared across all test classes: payload file loading,
 *    thread name logging.
 *
 * All test classes extend BaseTest. They do not need to call super() on
 * @BeforeMethod — TestNG calls lifecycle methods on parent classes
 * automatically.
 */
public class BaseTest {

    @BeforeSuite
    public void suiteSetup() {
        System.out.println("=".repeat(60));
        System.out.println("Test Suite Starting");
        System.out.println("JSON Base URL : " + ConfigManager.getJsonBaseUrl());
        System.out.println("XML  Base URL : " + ConfigManager.getXmlBaseUrl());
        System.out.println("=".repeat(60));
    }

    @BeforeMethod
    public void resetApiClient() {
        ApiClient.reset();
    }

    /**
     * Loads a payload file from the classpath and returns its content as a String.
     *
     * @param classpathPath e.g. "payloads/json/create-user.json"
     * @return file content as UTF-8 string
     */
    protected String loadPayload(String classpathPath) {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new IllegalArgumentException("Payload not found: " + classpathPath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load payload: " + classpathPath, e);
        }
    }

    protected String currentThread() {
        return Thread.currentThread().getName();
    }
}