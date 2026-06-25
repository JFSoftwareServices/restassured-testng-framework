package com.jide.framework.validators;

import io.restassured.response.Response;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * JsonSchemaValidator is a standalone helper for JSON Schema validation.
 *
 * RestAssured's built-in JSON Schema validation (via matchesJsonSchemaInClasspath)
 * is used here. This class exists to centralise the dependency on that import
 * and to allow validation to be called outside of a RestAssured response chain
 * if needed (e.g. validating a transformed response object).
 *
 * JSON Schema validation verifies the structure, types, and constraints of
 * a JSON response — not just individual field values. For example:
 *   - "id" must be an integer and must be present
 *   - "email" must be a string matching an email format
 *   - "name" must not be null
 * This catches API breaking changes that individual field assertions might miss.
 */
public class JsonSchemaValidator {

    private JsonSchemaValidator() {}

    /**
     * Validates a RestAssured Response body against a JSON Schema on the classpath.
     *
     * @param response      the RestAssured response to validate
     * @param classpathPath classpath path to the schema, e.g. "schemas/json/user-schema.json"
     */
    public static void validate(Response response, String classpathPath) {
        response.then().body(matchesJsonSchemaInClasspath(classpathPath));
    }
}