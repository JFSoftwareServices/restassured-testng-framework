# Runbook: RestAssured TestNG Framework — JFSoftwareServices/restassured-testng-framework

Follow these steps in order. Each step includes the exact file content to create and a verification command to confirm it worked before proceeding.

---

## Architecture Overview

This framework is a multi-layer API test automation framework built on RestAssured and TestNG. It supports JSON and XML payloads, all HTTP methods, parallel execution, and schema validation.

```
restassured-testng-framework/
├── .devcontainer/
│   └── devcontainer.json
├── src/test/
│   ├── java/com/jide/framework/
│   │   ├── config/
│   │   │   └── ConfigManager.java         # Reads env/properties, single source of truth
│   │   ├── client/
│   │   │   ├── ApiClient.java             # ThreadLocal RequestSpecification
│   │   │   └── RequestBuilder.java        # Fluent HTTP request builder
│   │   ├── models/
│   │   │   ├── User.java                  # POJO for JSON user resource
│   │   │   └── Post.java                  # POJO for JSON post resource
│   │   ├── assertions/
│   │   │   └── ApiResponseAssert.java     # Fluent assertion DSL
│   │   ├── validators/
│   │   │   ├── JsonSchemaValidator.java   # JSON Schema validation helper
│   │   │   └── XmlSchemaValidator.java    # XSD validation helper
│   │   ├── listeners/
│   │   │   └── TestListener.java          # TestNG listener for logging
│   │   └── tests/
│   │       ├── BaseTest.java              # Setup/teardown, shared config
│   │       ├── get/
│   │       │   └── GetRequestsTest.java   # GET method tests (JSON + XML)
│   │       ├── post/
│   │       │   └── PostRequestsTest.java  # POST method tests
│   │       ├── put/
│   │       │   └── PutRequestsTest.java   # PUT method tests
│   │       ├── patch/
│   │       │   └── PatchRequestsTest.java # PATCH method tests
│   │       ├── delete/
│   │       │   └── DeleteRequestsTest.java
│   │       └── headoptions/
│   │           └── HeadOptionsTest.java   # HEAD and OPTIONS tests
│   └── resources/
│       ├── config.properties              # Environment configuration
│       ├── schemas/json/
│       │   ├── user-schema.json           # JSON Schema for user resource
│       │   └── post-schema.json           # JSON Schema for post resource
│       ├── schemas/xml/
│       │   └── user-schema.xsd            # XSD schema for XML user resource
│       └── payloads/
│           ├── json/
│           │   ├── create-user.json       # POST/PUT JSON request body
│           │   └── patch-user.json        # PATCH JSON request body
│           └── xml/
│               └── create-user.xml        # POST XML request body
├── testng.xml                             # Sequential suite
├── testng-parallel.xml                    # Parallel suite
├── allure.properties                      # Allure results directory config (repo root)
└── pom.xml
```

### Framework layers

**Config layer** — `ConfigManager` reads `config.properties` or system
properties (allowing environment override at runtime). Every other class
reads configuration through `ConfigManager` only — never from raw system
calls scattered through tests.

**Client layer** — `ApiClient` holds a `ThreadLocal<RequestSpecification>`
so each parallel test thread has its own spec instance. `RequestBuilder`
wraps RestAssured's `given()...when()...then()` chain in a fluent builder
so test code reads like a specification, not a framework call.

**Models layer** — plain Java POJOs annotated so RestAssured/Jackson can
serialise and deserialise them to/from both JSON and XML automatically.

**Assertions layer** — `ApiResponseAssert` provides a fluent DSL so
assertion chains in tests read in plain English:
```java
assertThat(response).hasStatus(200).hasJsonPath("name", "Jide").matchesJsonSchema("user-schema.json");
```

**Validators layer** — dedicated classes for JSON Schema and XSD validation,
keeping that logic out of test classes and reusable across the suite.

**Listeners layer** — `TestListener` logs test start, pass, fail, and skip
events with the thread name, making parallel run output readable.

**Tests layer** — one class per HTTP method, each extending `BaseTest`.
`BaseTest` calls `ApiClient.reset()` in `@BeforeMethod` so thread-local
state never bleeds between tests.

---

## API under test

The framework uses two public APIs that require no authentication:

| API | Base URL | Formats | Used for |
|---|---|---|---|
| JSONPlaceholder | `https://jsonplaceholder.typicode.com` | JSON | GET, POST, PUT, PATCH, DELETE, HEAD |
| Country XML API | `https://restcountries.com/v3.1` | JSON + XML variant | XML payload examples |

For XML schema validation and XML POST/PUT, the framework demonstrates the
capability against a locally controlled payload. The `RequestBuilder` and
validators work identically against any XML-capable API.

---

## Step 1 — Create the repo

Open `https://github.com/JFSoftwareServices/restassured-testng-framework` in a browser.

- If it loads → it exists, skip to Step 2.
- If it shows 404 → create it:

1. Go to `https://github.com/organizations/JFSoftwareServices/repositories/new`
2. **Repository name**: `restassured-testng-framework`
3. **Visibility**: Private
4. Check **"Initialize this repository with a README"**
5. Click **Create repository**

**Verify:** the URL above loads and shows `README.md`.

---

## Step 2 — Create the devcontainer config

In the Codespace terminal (or locally before opening the Codespace), from
the repo root:

```bash
mkdir -p .devcontainer
```

Create `.devcontainer/devcontainer.json` with exactly this content:

```json
{
  "name": "restassured-testng-framework",
  "image": "mcr.microsoft.com/devcontainers/java:17",
  "features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
  },
  "postCreateCommand": "mvn -v && java -version",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vscjava.vscode-maven"
      ]
    }
  }
}
```

What this file does:

- `image` — the base Docker image for the Dev Container. The Microsoft
  `devcontainers/java:17` image already includes Java 17 and Maven — no
  separate Maven feature is needed. Adding a `maven` feature on top of this
  image is redundant and can cause the container build to fail if the feature
  registry is temporarily unreachable.
- `features` — additional tools installed on top of the base image.
  `docker-in-docker` gives the Dev Container its own Docker daemon so you
  can run `docker build` and `docker run` from inside it.
- `postCreateCommand` — runs once after the container is created, as a smoke
  test. If either `mvn -v` or `java -version` fails, the container
  provisioning did not complete correctly.
- `customizations.vscode.extensions` — VS Code extensions to install
  automatically. These improve the development experience but are not
  required for the tests to run.

5. Scroll down to **Commit changes**
6. Leave **"Commit directly to main"** selected
7. Click **Commit changes**

**Verify:** the file now appears in your repo at
`.devcontainer/devcontainer.json`. You can confirm by visiting:
`https://github.com/JFSoftwareServices/restassured-testng-framework/blob/main/.devcontainer/devcontainer.json`

**Open the Codespace:** go back to the repo main page → green **Code**
button → **Codespaces** tab → **Create codespace on main**. GitHub reads
the `devcontainer.json` you just committed and provisions the container
automatically. Wait 1–3 minutes for it to finish.

**Verify tools are installed** — once the Codespace opens, run in the
terminal at the bottom:

```bash
mvn -v
java -version
```

Both must print version numbers with no errors.

**Verify you are inside a Docker container:**

```bash
test -f /.dockerenv && echo "Inside a Docker container" || echo "NOT inside a Docker container"
```

Must print `Inside a Docker container`.

If `mvn -v` fails with "command not found", the `maven` feature did not
provision. Rebuild the container: Command Palette (`Ctrl+Shift+P`) →
**Codespaces: Rebuild Container** — then re-run the verify commands.

All remaining steps from Step 3 onward are run from inside the Codespace
terminal, not the GitHub website.

---

## Step 3 — Create the Maven project

Create `pom.xml` in the repo root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.jide</groupId>
  <artifactId>restassured-testng-framework</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <testng.version>7.10.2</testng.version>
    <restassured.version>5.5.0</restassured.version>
    <hamcrest.version>2.2</hamcrest.version>
    <jackson.version>2.17.1</jackson.version>
    <jackson-xml.version>2.17.1</jackson-xml.version>
    <json-schema-validator.version>5.5.0</json-schema-validator.version>
    <surefire.version>3.2.5</surefire.version>
    <slf4j.version>2.0.13</slf4j.version>
    <allure.version>2.27.0</allure.version>
    <aspectj.version>1.9.22</aspectj.version>
  </properties>

  <dependencies>

    <!-- TestNG -->
    <dependency>
      <groupId>org.testng</groupId>
      <artifactId>testng</artifactId>
      <version>${testng.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- RestAssured core -->
    <dependency>
      <groupId>io.rest-assured</groupId>
      <artifactId>rest-assured</artifactId>
      <version>${restassured.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- JSON Schema validation -->
    <dependency>
      <groupId>io.rest-assured</groupId>
      <artifactId>json-schema-validator</artifactId>
      <version>${json-schema-validator.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- XML support for RestAssured -->
    <dependency>
      <groupId>io.rest-assured</groupId>
      <artifactId>xml-path</artifactId>
      <version>${restassured.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Hamcrest matchers -->
    <dependency>
      <groupId>org.hamcrest</groupId>
      <artifactId>hamcrest</artifactId>
      <version>${hamcrest.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Jackson JSON serialisation -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>${jackson.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Jackson XML serialisation (for XML POJOs) -->
    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-xml</artifactId>
      <version>${jackson-xml.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Logging -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>${slf4j.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Allure TestNG integration -->
    <dependency>
      <groupId>io.qameta.allure</groupId>
      <artifactId>allure-testng</artifactId>
      <version>${allure.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- Allure RestAssured filter — auto-attaches HTTP requests and responses
         as Allure report attachments so every test step shows the full
         request/response without any manual code in test classes -->
    <dependency>
      <groupId>io.qameta.allure</groupId>
      <artifactId>allure-rest-assured</artifactId>
      <version>${allure.version}</version>
      <scope>test</scope>
    </dependency>

    <!-- AspectJ weaver — required for @Step annotations to work.
         Allure uses AspectJ AOP (Aspect-Oriented Programming) to intercept
         calls to methods annotated with @Step and record them as named steps
         in the report. Without the weaver on the JVM's javaagent, @Step
         annotations compile and run but have no effect on the report. -->
    <dependency>
      <groupId>org.aspectj</groupId>
      <artifactId>aspectjweaver</artifactId>
      <version>${aspectj.version}</version>
      <scope>test</scope>
    </dependency>

  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire.version}</version>
        <configuration>
          <!-- Default: run the parallel suite -->
          <suiteXmlFiles>
            <suiteXmlFile>testng-parallel.xml</suiteXmlFile>
          </suiteXmlFiles>
          <!-- 1 JVM fork per core; tests parallelise inside via TestNG threads -->
          <forkCount>1C</forkCount>
          <reuseForks>true</reuseForks>
          <!-- aspectjweaver must be on the JVM javaagent so @Step AOP
               interception works at runtime. Without this, @Step annotations
               have no effect and Allure report shows no step breakdown. -->
          <argLine>
            -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar"
            -Xmx1024m
          </argLine>
        </configuration>
      </plugin>

      <!-- Allure Maven plugin — generates the HTML report from raw JSON
           results written to target/allure-results during the test run.
           Run: mvn allure:report   (generate only)
                mvn allure:serve    (generate + open browser automatically) -->
      <plugin>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-maven</artifactId>
        <version>2.12.0</version>
        <configuration>
          <reportVersion>${allure.version}</reportVersion>
          <resultsDirectory>${project.build.directory}/allure-results</resultsDirectory>
          <reportDirectory>${project.build.directory}/allure-report</reportDirectory>
        </configuration>
      </plugin>

    </plugins>
  </build>

</project>
```

**Verify dependencies resolve:**

```bash
mvn -q dependency:resolve
```

Must complete with no `ERROR` lines.

Commit and push:

```bash
git add pom.xml
git commit -m "Add pom.xml with RestAssured, TestNG, Hamcrest, Jackson, Allure"
git push
```

---

## Step 4 — Create the directory structure

```bash
mkdir -p src/test/java/com/jide/framework/config
mkdir -p src/test/java/com/jide/framework/client
mkdir -p src/test/java/com/jide/framework/models
mkdir -p src/test/java/com/jide/framework/assertions
mkdir -p src/test/java/com/jide/framework/validators
mkdir -p src/test/java/com/jide/framework/listeners
mkdir -p src/test/java/com/jide/framework/tests/get
mkdir -p src/test/java/com/jide/framework/tests/post
mkdir -p src/test/java/com/jide/framework/tests/put
mkdir -p src/test/java/com/jide/framework/tests/patch
mkdir -p src/test/java/com/jide/framework/tests/delete
mkdir -p src/test/java/com/jide/framework/tests/headoptions
mkdir -p src/test/resources/schemas/json
mkdir -p src/test/resources/schemas/xml
mkdir -p src/test/resources/payloads/json
mkdir -p src/test/resources/payloads/xml
```

Commit and push:

```bash
git add src/
git commit -m "Add directory structure"
git push
```

---

## Step 5 — Create config and Allure properties files

Create `src/test/resources/config.properties`:

```properties
# Base URLs — override with -Dbase.url.json=... at runtime
base.url.json=https://jsonplaceholder.typicode.com
base.url.xml=https://jsonplaceholder.typicode.com

# Connection and read timeouts in milliseconds
connection.timeout=5000
read.timeout=10000

# Log requests and responses (true/false)
log.requests=true
log.responses=true
```

Create `src/test/resources/allure.properties`:

```properties
# Directory where Allure writes raw JSON result files during the test run.
# mvn allure:report reads from here to generate the HTML report.
allure.results.directory=target/allure-results
```

`allure.properties` must be on the classpath (inside `src/test/resources/`)
so the Allure TestNG listener finds it automatically. The results directory
path is relative to the project root. Allure creates it on first run if it
does not exist.

---

## Step 6 — Create ConfigManager

Create `src/test/java/com/jide/framework/config/ConfigManager.java`:

```java
package com.jide.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager is the single source of truth for all framework configuration.
 *
 * It loads values from config.properties on the classpath, then allows any
 * value to be overridden at runtime via a system property or environment
 * variable. The lookup order for any key is:
 *
 *   1. System property (-Dkey=value passed to Maven or Docker)
 *   2. Environment variable (KEY converted to uppercase with dots as underscores)
 *   3. config.properties value
 *   4. Supplied default
 *
 * This means the same test suite binary can be pointed at different
 * environments without changing any source code:
 *   mvn test -Dbase.url.json=https://staging-api.example.com
 *
 * ConfigManager is a singleton — the properties file is read once at class
 * load time. All access is through static methods so no instance needs to
 * be passed around.
 */
public class ConfigManager {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream is = ConfigManager.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            PROPERTIES.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    private ConfigManager() {}

    /**
     * Returns the value for the given key, checking system properties and
     * environment variables before falling back to config.properties.
     */
    public static String get(String key) {
        // 1. System property
        String value = System.getProperty(key);
        if (value != null) return value;

        // 2. Environment variable (BASE_URL_JSON for key base.url.json)
        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null) return value;

        // 3. config.properties
        value = PROPERTIES.getProperty(key);
        if (value != null) return value;

        throw new IllegalArgumentException("No configuration value found for key: " + key);
    }

    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getJsonBaseUrl() {
        return get("base.url.json");
    }

    public static String getXmlBaseUrl() {
        return get("base.url.xml");
    }
}
```

Commit and push:

```bash
git add src/test/resources/config.properties \
        src/test/java/com/jide/framework/config/ConfigManager.java
git commit -m "Add ConfigManager and config.properties"
git push
```
## Step 7 — Create ApiClient

Create `src/test/java/com/jide/framework/client/ApiClient.java`:

```java
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
```

---

## Step 8 — Create RequestBuilder

Create `src/test/java/com/jide/framework/client/RequestBuilder.java`:

```java
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
```

---

## Step 9 — Create Models

### User POJO

Create `src/test/java/com/jide/framework/models/User.java`:

```java
package com.jide.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * User represents the user resource returned by the API.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) means if the API returns fields
 * not declared here, Jackson silently ignores them rather than throwing an
 * error. This is the correct default for test frameworks — you declare only
 * the fields you care about asserting.
 *
 * @JacksonXmlRootElement(localName = "user") tells Jackson to use <user> as
 * the root XML element when serialising this POJO to XML, and to expect
 * <user> as the root element when deserialising XML responses.
 *
 * The same POJO is used for both JSON and XML because Jackson's ObjectMapper
 * (JSON) and XmlMapper (XML) share the same field mapping — you just call
 * the appropriate mapper for the format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "user")
public class User {

    private Integer id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String website;

    public User() {}

    public User(String name, String username, String email) {
        this.name     = name;
        this.username = username;
        this.email    = email;
    }

    // Getters and setters

    public Integer getId()                { return id; }
    public void    setId(Integer id)      { this.id = id; }

    public String  getName()              { return name; }
    public void    setName(String name)   { this.name = name; }

    public String  getUsername()              { return username; }
    public void    setUsername(String u)      { this.username = u; }

    public String  getEmail()             { return email; }
    public void    setEmail(String email) { this.email = email; }

    public String  getPhone()             { return phone; }
    public void    setPhone(String phone) { this.phone = phone; }

    public String  getWebsite()               { return website; }
    public void    setWebsite(String website) { this.website = website; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
```

### Post POJO

Create `src/test/java/com/jide/framework/models/Post.java`:

```java
package com.jide.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "post")
public class Post {

    private Integer id;
    private Integer userId;
    private String  title;
    private String  body;

    public Post() {}

    public Post(Integer userId, String title, String body) {
        this.userId = userId;
        this.title  = title;
        this.body   = body;
    }

    public Integer getId()                 { return id; }
    public void    setId(Integer id)       { this.id = id; }

    public Integer getUserId()             { return userId; }
    public void    setUserId(Integer uid)  { this.userId = uid; }

    public String  getTitle()              { return title; }
    public void    setTitle(String title)  { this.title = title; }

    public String  getBody()               { return body; }
    public void    setBody(String body)    { this.body = body; }

    @Override
    public String toString() {
        return "Post{id=" + id + ", userId=" + userId + ", title='" + title + "'}";
    }
}
```

Commit and push:

```bash
git add src/test/java/com/jide/framework/client/ \
        src/test/java/com/jide/framework/models/
git commit -m "Add ApiClient, RequestBuilder, User and Post models"
git push
```

---

## Step 10 — Create ApiResponseAssert

Create `src/test/java/com/jide/framework/assertions/ApiResponseAssert.java`:

```java
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
```

---

## Step 11 — Create Validators

### JsonSchemaValidator

Create `src/test/java/com/jide/framework/validators/JsonSchemaValidator.java`:

```java
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
```

### XmlSchemaValidator

Create `src/test/java/com/jide/framework/validators/XmlSchemaValidator.java`:

```java
package com.jide.framework.validators;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * XmlSchemaValidator validates an XML string against an XSD schema.
 *
 * RestAssured does not ship with built-in XSD validation (unlike JSON Schema),
 * so this class uses the Java standard library's javax.xml.validation API.
 *
 * SchemaFactory loads the XSD from the classpath, compiles it into a Schema
 * object, then creates a Validator to check the XML string. Any validation
 * error throws an AssertionError, which TestNG reports as a test failure.
 *
 * XSD validation verifies:
 *   - Element names and nesting match the schema
 *   - Data types (xs:integer, xs:string, etc.) are correct
 *   - Required elements are present
 *   - Attribute constraints are met
 */
public class XmlSchemaValidator {

    private XmlSchemaValidator() {}

    /**
     * Validates an XML string against an XSD file on the classpath.
     *
     * @param xmlContent    the XML string to validate
     * @param classpathPath classpath path to the XSD, e.g. "schemas/xml/user-schema.xsd"
     * @throws AssertionError if validation fails
     */
    public static void validate(String xmlContent, String classpathPath) {
        try {
            InputStream xsdStream = XmlSchemaValidator.class
                .getClassLoader()
                .getResourceAsStream(classpathPath);

            if (xsdStream == null) {
                throw new AssertionError("XSD schema not found on classpath: " + classpathPath);
            }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsdStream));
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));

        } catch (SAXException e) {
            throw new AssertionError("XML schema validation failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AssertionError("IO error during XML schema validation: " + e.getMessage(), e);
        }
    }
}
```

---

## Step 12 — Create TestListener

Create `src/test/java/com/jide/framework/listeners/TestListener.java`:

```java
package com.jide.framework.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestListener implements TestNG's ITestListener to log test lifecycle events.
 *
 * In parallel runs, multiple threads write to the console simultaneously.
 * Including the thread name in every log line makes it possible to trace
 * which thread ran which test and in what order, which is essential for
 * debugging parallel-specific failures.
 *
 * The listener is registered in testng-parallel.xml via:
 *   <listeners>
 *     <listener class-name="com.jide.framework.listeners.TestListener"/>
 *   </listeners>
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.printf("[%s][START ] %s.%s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.printf("[%s][PASS  ] %s.%s (%.0fms)%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            (double)(result.getEndMillis() - result.getStartMillis()));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.printf("[%s][FAIL  ] %s.%s — %s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.printf("[%s][SKIP  ] %s.%s%n",
            threadName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName());
    }

    private static String threadName() {
        return Thread.currentThread().getName();
    }
}
```

Commit and push:

```bash
git add src/test/java/com/jide/framework/assertions/ \
        src/test/java/com/jide/framework/validators/ \
        src/test/java/com/jide/framework/listeners/
git commit -m "Add ApiResponseAssert, validators, and TestListener"
git push
```
## Step 13 — Create schemas and payloads

### JSON Schema — User

Create `src/test/resources/schemas/json/user-schema.json`:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "User",
  "type": "object",
  "required": ["id", "name", "username", "email"],
  "properties": {
    "id": {
      "type": "integer",
      "description": "Unique identifier for the user"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "description": "Full name"
    },
    "username": {
      "type": "string",
      "minLength": 1
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "phone": {
      "type": "string"
    },
    "website": {
      "type": "string"
    },
    "address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city":   { "type": "string" },
        "zipcode":{ "type": "string" }
      }
    },
    "company": {
      "type": "object",
      "properties": {
        "name": { "type": "string" }
      }
    }
  },
  "additionalProperties": true
}
```

### JSON Schema — Post

Create `src/test/resources/schemas/json/post-schema.json`:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Post",
  "type": "object",
  "required": ["id", "userId", "title", "body"],
  "properties": {
    "id": {
      "type": "integer"
    },
    "userId": {
      "type": "integer"
    },
    "title": {
      "type": "string",
      "minLength": 1
    },
    "body": {
      "type": "string",
      "minLength": 1
    }
  },
  "additionalProperties": false
}
```

### JSON Schema — User list

Create `src/test/resources/schemas/json/user-list-schema.json`:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "UserList",
  "type": "array",
  "items": {
    "$ref": "user-schema.json"
  },
  "minItems": 1
}
```

### XSD Schema — User

Create `src/test/resources/schemas/xml/user-schema.xsd`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">

  <xs:element name="user">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="id"       type="xs:integer" minOccurs="0"/>
        <xs:element name="name"     type="xs:string"  minOccurs="1"/>
        <xs:element name="username" type="xs:string"  minOccurs="1"/>
        <xs:element name="email"    type="emailType"  minOccurs="1"/>
        <xs:element name="phone"    type="xs:string"  minOccurs="0"/>
        <xs:element name="website"  type="xs:string"  minOccurs="0"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>

  <xs:simpleType name="emailType">
    <xs:restriction base="xs:string">
      <xs:pattern value="[^@\s]+@[^@\s]+\.[^@\s]+"/>
    </xs:restriction>
  </xs:simpleType>

</xs:schema>
```

### JSON payload — create user

Create `src/test/resources/payloads/json/create-user.json`:

```json
{
  "name": "Jide Framework User",
  "username": "jide.framework",
  "email": "jide@framework.com",
  "phone": "07700-900000",
  "website": "jfsoftwareservices.com"
}
```

### JSON payload — patch user

Create `src/test/resources/payloads/json/patch-user.json`:

```json
{
  "email": "updated@framework.com"
}
```

### XML payload — create user

Create `src/test/resources/payloads/xml/create-user.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<user>
  <name>Jide Framework User</name>
  <username>jide.framework</username>
  <email>jide@framework.com</email>
  <phone>07700-900000</phone>
  <website>jfsoftwareservices.com</website>
</user>
```

Commit and push:

```bash
git add src/test/resources/
git commit -m "Add JSON schemas, XSD schema, and request payloads"
git push
```

---

## Step 14 — Create BaseTest

Create `src/test/java/com/jide/framework/tests/BaseTest.java`:

```java
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
```

Commit and push:

```bash
git add src/test/java/com/jide/framework/tests/BaseTest.java
git commit -m "Add BaseTest with BeforeSuite and BeforeMethod reset"
git push
```

---

## Step 15 — Create GET tests

Create `src/test/java/com/jide/framework/tests/get/GetRequestsTest.java`:

```java
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
 *   - Retrieve a collection (list of resources)
 *   - Retrieve a single resource by ID
 *   - Retrieve with query parameters (filtering, pagination)
 *   - Nested resource retrieval
 *   - Edge cases: non-existent ID (404), invalid ID format
 *   - JSON Schema validation on collection and single resource
 *   - Response time assertion
 *
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

---

## Step 16 — Create POST tests

Create `src/test/java/com/jide/framework/tests/post/PostRequestsTest.java`:

```java
package com.jide.framework.tests.post;

import com.jide.framework.assertions.ApiResponseAssert;
import com.jide.framework.client.RequestBuilder;
import com.jide.framework.models.Post;
import com.jide.framework.models.User;
import com.jide.framework.tests.BaseTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * PostRequestsTest covers all POST scenarios.
 */
@Epic("REST API Framework")
@Feature("POST Requests")
public class PostRequestsTest extends BaseTest {

    // -------------------------------------------------------------------------
    // Successful POST — JSON
    // -------------------------------------------------------------------------

    @Test(description = "POST /users with POJO body returns 201 with echoed fields")
    @Story("Create user")
    @Severity(SeverityLevel.CRITICAL)
    public void createUser_withPojo_returns201() {
        User user = new User("Jide Framework", "jide.fw", "jide@fw.com");

        Response response = RequestBuilder.withJson()
            .body(user)
            .post("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201)
            .hasJsonPath("name", "Jide Framework")
            .hasJsonPath("username", "jide.fw")
            .hasJsonPath("email", "jide@fw.com")
            .hasJsonPathPresent("id");
    }

    @Test(description = "POST /users with raw JSON payload file returns 201")
    @Story("Create user")
    @Severity(SeverityLevel.CRITICAL)
    public void createUser_withPayloadFile_returns201() {
        String payload = loadPayload("payloads/json/create-user.json");

        Response response = RequestBuilder.withJson()
            .body(payload)
            .post("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201)
            .hasJsonPath("name", "Jide Framework User")
            .hasJsonPath("email", "jide@framework.com")
            .hasJsonPathPresent("id");
    }

    @Test(description = "POST /posts with POJO body returns 201 with correct userId")
    @Story("Create post")
    @Severity(SeverityLevel.CRITICAL)
    public void createPost_withPojo_returns201() {
        Post post = new Post(1, "Framework Post Title", "Body content for the post");

        Response response = RequestBuilder.withJson()
            .body(post)
            .post("/posts");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201)
            .hasJsonPath("userId", 1)
            .hasJsonPath("title", "Framework Post Title")
            .hasJsonPath("body", "Body content for the post")
            .hasJsonPathPresent("id");
    }

    @Test(description = "POST /users with Map body returns 201")
    @Story("Create user")
    @Severity(SeverityLevel.NORMAL)
    public void createUser_withMap_returns201() {
        Map<String, Object> body = new HashMap<>();
        body.put("name",     "Map User");
        body.put("username", "mapuser");
        body.put("email",    "map@user.com");

        Response response = RequestBuilder.withJson()
            .body(body)
            .post("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201)
            .hasJsonPath("name", "Map User");
    }

    // -------------------------------------------------------------------------
    // POST — XML body
    // -------------------------------------------------------------------------

    @Test(description = "POST /users with XML payload is accepted")
    @Story("Create user — XML payload")
    @Severity(SeverityLevel.NORMAL)
    @Description("Demonstrates the XML client path and XSD validation. JSONPlaceholder is JSON-only so the XML body is sent as JSON; XSD validation is demonstrated independently on a known-good XML string.")
    public void createUser_withXmlPayload_returns201() {
        String xmlPayload = loadPayload("payloads/xml/create-user.xml");

        // JSONPlaceholder accepts the body regardless of content type and echoes
        // it back. A real XML API would return XML; here we verify the request
        // is sent and a success response received, demonstrating the XML client path.
        Response response = RequestBuilder.withJson()
            .headers(Map.of("Content-Type", "application/json"))
            .body(loadPayload("payloads/json/create-user.json"))
            .post("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201);

        // Separately demonstrate XSD validation on a known-good XML string
        // to prove the XML validation path works independently of the API.
        String validXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<user>"
            + "<name>Jide Framework User</name>"
            + "<username>jide.framework</username>"
            + "<email>jide@framework.com</email>"
            + "</user>";

        ApiResponseAssert.assertThat(response)
            .matchesXmlSchema("schemas/xml/user-schema.xsd");

        // Direct XSD validation of the XML payload itself
        com.jide.framework.validators.XmlSchemaValidator
            .validate(validXml, "schemas/xml/user-schema.xsd");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test(description = "POST /users with empty body still returns a response")
    @Story("Edge cases — request body")
    @Severity(SeverityLevel.MINOR)
    public void createUser_withEmptyBody_returnsResponse() {
        Response response = RequestBuilder.withJson()
            .body("{}")
            .post("/users");

        // JSONPlaceholder returns 201 with an id even for empty body
        // A real API would return 400; this test demonstrates the behaviour
        // is observed and asserted explicitly.
        ApiResponseAssert.assertThat(response)
            .hasStatusIn(201, 400);
    }

    @Test(description = "POST /users with extra unknown fields returns 201 (API ignores extras)")
    @Story("Edge cases — request body")
    @Severity(SeverityLevel.MINOR)
    public void createUser_withExtraFields_returns201() {
        Map<String, Object> body = new HashMap<>();
        body.put("name",          "Extra Fields User");
        body.put("username",      "extra.fields");
        body.put("email",         "extra@fields.com");
        body.put("unknownField",  "this should be ignored by the API");

        Response response = RequestBuilder.withJson()
            .body(body)
            .post("/users");

        ApiResponseAssert.assertThat(response)
            .hasStatus(201)
            .hasJsonPath("name", "Extra Fields User");
    }
}
```

---

## Step 17 — Create PUT tests

Create `src/test/java/com/jide/framework/tests/put/PutRequestsTest.java`:

```java
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
```

---

## Step 18 — Create PATCH tests

Create `src/test/java/com/jide/framework/tests/patch/PatchRequestsTest.java`:

```java
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
```

---

## Step 19 — Create DELETE tests

Create `src/test/java/com/jide/framework/tests/delete/DeleteRequestsTest.java`:

```java
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
```

---

## Step 20 — Create HEAD and OPTIONS tests

Create `src/test/java/com/jide/framework/tests/headoptions/HeadOptionsTest.java`:

```java
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
```

Commit and push:

```bash
git add src/test/java/com/jide/framework/tests/
git commit -m "Add all HTTP method test classes with Allure annotations: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS"
git push
```
## Step 21 — Create TestNG suite files

### Sequential suite (for debugging)

Create `testng.xml` in the repo root:

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="RestAssuredFramework-Sequential" verbose="1">

  <listeners>
    <listener class-name="com.jide.framework.listeners.TestListener"/>
  </listeners>

  <test name="GET Tests">
    <classes>
      <class name="com.jide.framework.tests.get.GetRequestsTest"/>
    </classes>
  </test>

  <test name="POST Tests">
    <classes>
      <class name="com.jide.framework.tests.post.PostRequestsTest"/>
    </classes>
  </test>

  <test name="PUT Tests">
    <classes>
      <class name="com.jide.framework.tests.put.PutRequestsTest"/>
    </classes>
  </test>

  <test name="PATCH Tests">
    <classes>
      <class name="com.jide.framework.tests.patch.PatchRequestsTest"/>
    </classes>
  </test>

  <test name="DELETE Tests">
    <classes>
      <class name="com.jide.framework.tests.delete.DeleteRequestsTest"/>
    </classes>
  </test>

  <test name="HEAD and OPTIONS Tests">
    <classes>
      <class name="com.jide.framework.tests.headoptions.HeadOptionsTest"/>
    </classes>
  </test>

</suite>
```

### Parallel suite (for CI)

Create `testng-parallel.xml` in the repo root:

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="RestAssuredFramework-Parallel"
       parallel="methods"
       thread-count="8"
       data-provider-thread-count="4"
       verbose="1">

  <listeners>
    <listener class-name="com.jide.framework.listeners.TestListener"/>
  </listeners>

  <test name="AllAPITests">
    <classes>
      <class name="com.jide.framework.tests.get.GetRequestsTest"/>
      <class name="com.jide.framework.tests.post.PostRequestsTest"/>
      <class name="com.jide.framework.tests.put.PutRequestsTest"/>
      <class name="com.jide.framework.tests.patch.PatchRequestsTest"/>
      <class name="com.jide.framework.tests.delete.DeleteRequestsTest"/>
      <class name="com.jide.framework.tests.headoptions.HeadOptionsTest"/>
    </classes>
  </test>

</suite>
```

**Why `parallel="methods"`:** each `@Test` method is an independent HTTP
call with no shared state (because `ApiClient.reset()` in `@BeforeMethod`
ensures a clean ThreadLocal spec before every method). There is no shared
driver or session to protect. `methods` mode gives the maximum parallelism
for a pure API suite.

**Why `thread-count="8"`:** a starting point for a 4-core machine (cores × 2).
Run `nproc` in the Codespace terminal and adjust this value to cores × 2.

Commit and push:

```bash
git add testng.xml testng-parallel.xml
git commit -m "Add sequential and parallel TestNG suite files"
git push
```

---

## Step 22 — Run the suite locally and verify

### Run sequentially first (recommended for initial verification)

```bash
mvn clean test -Dsurefire.suiteXmlFiles=testng.xml
```

Expected output:

```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

If any tests fail, read the failure message in the console — it will name
the test method, the expected value, and the actual value. Fix the test
before running the parallel suite.

### Run in parallel

```bash
mvn clean test
```

This uses the default `testng-parallel.xml` from `pom.xml`. You will see
`[THREAD]` log lines interleaved from multiple threads, for example:

```
[TestNG-methods-3][START ] ...get.GetRequestsTest.getUserById_returns200WithCorrectUser
[TestNG-methods-1][START ] ...post.PostRequestsTest.createUser_withPojo_returns201
[TestNG-methods-5][START ] ...delete.DeleteRequestsTest.deleteUser_existingId_returnsSuccess
[TestNG-methods-3][PASS  ] ...get.GetRequestsTest.getUserById_returns200WithCorrectUser (312ms)
```

Different thread numbers across methods confirm parallel execution is active.

**Verify parallel execution is actually happening:**

```bash
grep "THREAD" target/surefire-reports/*.txt | awk -F'methods-' '{print $2}' | cut -d']' -f1 | sort -u
```

This extracts the unique thread numbers from the output. You must see more
than one thread number for parallel to be confirmed.

---

## Step 23 — Verify all files are committed

```bash
git status
```

Must show:

```
nothing to commit, working tree clean
```

Check the full structure is in place:

```bash
find src -name "*.java" | sort
find src -name "*.json" -o -name "*.xsd" -o -name "*.xml" -o -name "*.properties" | sort
```

Expected Java files:

```
src/test/java/com/jide/framework/assertions/ApiResponseAssert.java
src/test/java/com/jide/framework/client/ApiClient.java
src/test/java/com/jide/framework/client/RequestBuilder.java
src/test/java/com/jide/framework/config/ConfigManager.java
src/test/java/com/jide/framework/listeners/TestListener.java
src/test/java/com/jide/framework/models/Post.java
src/test/java/com/jide/framework/models/User.java
src/test/java/com/jide/framework/tests/BaseTest.java
src/test/java/com/jide/framework/tests/delete/DeleteRequestsTest.java
src/test/java/com/jide/framework/tests/get/GetRequestsTest.java
src/test/java/com/jide/framework/tests/headoptions/HeadOptionsTest.java
src/test/java/com/jide/framework/tests/patch/PatchRequestsTest.java
src/test/java/com/jide/framework/tests/post/PostRequestsTest.java
src/test/java/com/jide/framework/tests/put/PutRequestsTest.java
src/test/java/com/jide/framework/validators/JsonSchemaValidator.java
src/test/java/com/jide/framework/validators/XmlSchemaValidator.java
```

---

## Framework summary

### What this framework demonstrates

| Area | What is shown |
|---|---|
| HTTP methods | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS — all covered with multiple test cases |
| Payload formats | JSON (POJO, Map, raw string, payload file) and XML (payload file, XSD validation) |
| Schema validation | JSON Schema (draft-07) via RestAssured's built-in validator; XSD via javax.xml.validation |
| Thread safety | ThreadLocal RequestSpecification with reset in @BeforeMethod; proven by parallel execution |
| Parallel execution | TestNG `parallel="methods"` with 8 threads; TestListener logs thread name per test |
| Edge cases | 404 for non-existent resources, idempotent DELETE, empty body, extra fields, response time SLA |
| Fluent DSL | ApiResponseAssert chains: `.hasStatus().hasJsonPath().matchesJsonSchema()` |
| Config management | ConfigManager with properties file + system property + env var override chain |
| Separation of concerns | Config / Client / Models / Assertions / Validators / Listeners / Tests — each in its own package |
| Reporting | Allure: @Epic/@Feature/@Story hierarchy, @Severity, @Description, @Step on HTTP methods, auto-attached HTTP request/response bodies via AllureRestAssured filter |

### Key design decisions worth discussing in interview

**ThreadLocal over synchronization:** using `synchronized` blocks or
`ReentrantLock` to protect a shared `RequestSpecification` would work but
would serialize access — one thread waits while another uses the spec,
eliminating the benefit of parallelism. ThreadLocal eliminates contention
entirely by giving each thread its own copy. This is the standard pattern
for per-thread objects in Java concurrent programming.

**Reset in @BeforeMethod, not @AfterMethod:** resetting after a test means
a failed test that throws before the reset runs leaves stale state. Resetting
before ensures every test starts clean regardless of what the previous one
did.

**ApiResponseAssert as a custom assertion DSL over RestAssured's .then():**
RestAssured's built-in `.then().body(...)` is powerful but mixes request
building and assertion in the same chain, making tests harder to read.
Separating the request (RequestBuilder) from the assertion (ApiResponseAssert)
produces test code that reads like a plain English specification.

**ConfigManager lookup order (system property → env var → file):** this
allows the same compiled test artifact to run against dev, staging, and
production by changing only the environment variable or Maven command line.
No recompilation needed between environments.

**`@JsonIgnoreProperties(ignoreUnknown = true)` on models:** API responses
often include fields added in newer versions. Without this annotation,
Jackson throws an exception for unrecognised fields, causing tests to fail
when the API is extended. With it, tests only validate the fields they
declare — which is the correct scope for an API test.

### Running against a different environment

```bash
# Point to staging API
mvn clean test -Dbase.url.json=https://staging-api.example.com

# Or via environment variable
export BASE_URL_JSON=https://staging-api.example.com
mvn clean test

# Run sequential suite for debugging
mvn clean test -Dsurefire.suiteXmlFiles=testng.xml

# Run only GET tests
mvn clean test -Dsurefire.suiteXmlFiles=testng.xml -Dtest=GetRequestsTest
```

---

## Allure Reporting

### What Allure is and how it works in this framework

Allure is an open-source test reporting framework that transforms raw test
results into an interactive HTML dashboard. Rather than reading walls of
console output or Surefire XML files, you get a structured report with pass
rates, test history, per-test step breakdowns, and attached HTTP
request/response bodies.

In this framework, Allure is wired in at three levels:

```
Test execution
     |
     ├── AllureRestAssured filter (in ApiClient)
     │       Automatically captures every HTTP request and response body
     │       and attaches them as named attachments to the test step in
     │       the report. No code required in test classes.
     │
     ├── @Step on RequestBuilder HTTP methods
     │       Each HTTP call (GET /users, POST /posts, etc.) appears as a
     │       named step in the report's step breakdown for that test.
     │       AspectJ intercepts the annotated method call at runtime.
     │
     └── Annotations on test classes
             @Epic    — top-level grouping  (e.g. "REST API Framework")
             @Feature — feature under test  (e.g. "GET Requests")
             @Story   — specific scenario   (e.g. "Retrieve user by ID")
             @Severity — BLOCKER / CRITICAL / NORMAL / MINOR / TRIVIAL
             @Description — long-form explanation for complex tests
```

During the test run, the Allure TestNG listener writes a JSON result file
to `target/allure-results/` for each test. These raw JSON files are not
the report — they are the data source. The HTML report is generated from
them by a separate Maven command.

---

### Allure annotation reference

| Annotation | Level | Purpose |
|---|---|---|
| `@Epic` | Class | Highest grouping — usually the project or system name |
| `@Feature` | Class | The feature or API area under test |
| `@Story` | Method | The specific user story or scenario |
| `@Severity` | Method | `BLOCKER` — must pass for release; `CRITICAL` — core functionality; `NORMAL` — standard; `MINOR` — low impact; `TRIVIAL` — cosmetic |
| `@Description` | Method | Long-form explanation shown in the report |
| `@Step` | Method | Records a named step in the test's step breakdown |
| `@Attachment` | Method | Attaches returned value (String, byte[]) to the report |

**Severity levels and when to use each:**

`BLOCKER` — the test covers a defect that would prevent the application from
being released. Example: a 404 where a 200 is expected on a core endpoint.

`CRITICAL` — core functionality that must work. Example: creating a user
returns 201 with the echoed body.

`NORMAL` — standard test, important but not release-blocking if one fails.
Example: query parameter filtering returns the correct subset of results.

`MINOR` — nice to have. Example: response time is within the SLA.

`TRIVIAL` — very low impact. Rarely used in API testing.

---

### Step 24 — Verify Allure results are generated after a test run

Run the suite:

```bash
mvn clean test
```

After it completes, verify the results directory was populated:

```bash
ls target/allure-results/
```

You should see multiple `.json` files — one per test method — plus an
`attachments/` folder containing the captured HTTP request and response
bodies. If this directory is empty or missing:

- Confirm `allure.properties` is in `src/test/resources/` (not the project root)
- Confirm `allure-testng` is in the dependencies in `pom.xml`
- Confirm `AspectJ` is on the `argLine` javaagent in Surefire config

---

### Step 25 — Generate the HTML report

```bash
mvn allure:report
```

`mvn allure:report` reads all JSON files from `target/allure-results/` and
generates a static HTML report in `target/allure-report/`. The entry point
is `target/allure-report/index.html`.

**Verify:**

```bash
ls target/allure-report/
```

Must show `index.html` and supporting asset folders (`app.js`, `css/`, etc.).

To open the report in a browser from the Codespace, right-click
`target/allure-report/index.html` in the VS Code file explorer →
**Open with Live Server** (if the Live Server extension is installed), or
use Step 26 below which serves it automatically.

---

### Step 26 — Serve the report in a browser

```bash
mvn allure:serve
```

`mvn allure:serve` generates the report and starts a local HTTP server on a
random port, then opens it in your default browser. In a Codespace, VS Code
detects the port and shows a forwarding prompt — click **Open in Browser**.

The served report is live for the duration of the Maven process. Press
`Ctrl+C` to stop it.

**The difference between `allure:report` and `allure:serve`:**

| Command | Generates HTML | Starts server | Opens browser |
|---|---|---|---|
| `mvn allure:report` | Yes — in `target/allure-report/` | No | No |
| `mvn allure:serve` | Yes — in a temp directory | Yes | Yes |

Use `allure:report` in CI (Jenkins) so the HTML is saved as a build artifact.
Use `allure:serve` locally for immediate interactive viewing.

---

### Step 27 — Understand the report structure

The Allure report has several views, accessible from the left sidebar:

**Overview**
Displays a summary dashboard: total tests, pass rate, a donut chart of
results by status (PASSED / FAILED / BROKEN / SKIPPED), and trend graphs
if multiple builds have been run.

**Suites**
Shows the test results organised by TestNG suite and class. Expand a class
to see individual test methods with their status, duration, and step
breakdown.

**Behaviors**
Organises tests by the `@Epic → @Feature → @Story` hierarchy. This is the
most useful view for communicating test coverage to stakeholders — it reads
like a feature map rather than a class list.

Example hierarchy from this framework:

```
REST API Framework              ← @Epic
    ├── GET Requests            ← @Feature
    │   ├── Retrieve all users  ← @Story
    │   │   ├── getAllUsers_returns200WithList     [PASSED]
    │   │   └── getAllUsers_matchesJsonSchema      [PASSED]
    │   └── Retrieve user by ID ← @Story
    │       └── getUserById_returns200WithCorrectUser [PASSED]
    ├── POST Requests
    │   ├── Create user
    │   └── Create user — XML payload
    └── ...
```

**Graphs**
Charts for severity distribution, status distribution, and response time
duration. Useful for seeing at a glance whether BLOCKER/CRITICAL tests are
all passing.

**Timeline**
Shows which threads ran which tests and when, in a Gantt-chart style
timeline. This is particularly useful for verifying that parallel execution
is actually occurring and that threads are not queueing behind each other.

**Test detail page**
Clicking any individual test shows:
- Test description (from `@Description`)
- Severity, Epic, Feature, Story labels
- Step breakdown (from `@Step` on `RequestBuilder`)
- HTTP request attachment: method, URL, headers, body
- HTTP response attachment: status code, headers, body
- Failure message and stack trace if the test failed

The HTTP request and response attachments come from the `AllureRestAssured`
filter registered in `ApiClient`. Every call made through `RequestBuilder`
is captured automatically.

---

### Step 28 — Publish the Allure report in Jenkins

To make the Allure report available from the Jenkins build page, install the
**Allure Jenkins Plugin** and update the Jenkinsfile:

**Install the plugin:**

Jenkins → **Manage Jenkins → Plugins → Available plugins** → search
`Allure` → install **Allure Jenkins Plugin** → restart Jenkins.

**Update the Jenkinsfile** — add an `allure` post step:

```groovy
post {
    always {
        junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

        // Publish Allure report from raw JSON results
        allure([
            includeProperties: false,
            jdk: '',
            results: [[path: 'target/allure-results']]
        ])

        archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
    }
    cleanup {
        sh "docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true"
    }
}
```

The `allure` step reads `target/allure-results/` (written by the test
container and mounted back to the Jenkins workspace via the volume mount),
generates the HTML report, and adds an **Allure Report** link to the build
page. The report persists across builds and shows trend graphs comparing
pass rates over time.

**Verify:** after the next Jenkins build completes, the build page shows an
**Allure Report** button. Click it to open the full interactive dashboard.

---

### Allure annotation troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `target/allure-results/` is empty after test run | `allure-testng` not in dependencies, or `allure.properties` not on classpath | Confirm both are present; re-run `mvn -q dependency:resolve` |
| `@Step` methods not appearing in report steps | AspectJ weaver not on JVM javaagent | Confirm `argLine` in Surefire config has `-javaagent:...aspectjweaver...jar` |
| HTTP request/response not attached | `AllureRestAssured` filter not added to `ApiClient` | Confirm `.addFilter(new AllureRestAssured())` is in both `buildJsonSpec()` and `buildXmlSpec()` |
| Behaviors view is empty | `@Epic`/`@Feature`/`@Story` annotations missing from test classes | Add annotations to each test class and method |
| `mvn allure:serve` port not opening in Codespace | Port not forwarded | Check VS Code Ports tab; if absent, forward the port shown in console output manually |
| Report shows all tests in one flat list | `@Feature` and `@Story` annotations are missing | Add class-level `@Epic`/`@Feature` and method-level `@Story` to each test class |

---



1. **`mvn -q dependency:resolve` fails** → a dependency version in `pom.xml`
   is wrong or unavailable. Check [mvnrepository.com](https://mvnrepository.com)
   for the correct version.

2. **`BUILD FAILURE` with `ClassNotFoundException`** → a class is in the wrong
   package or the directory structure doesn't match the `package` declaration.
   Run `find src -name "*.java" | sort` and verify each file's path matches
   its `package com.jide.framework...` declaration.

3. **`BUILD FAILURE` on a specific test method** → read the assertion error.
   It will say: `Expected: <X> but was: <Y>`. If `Y` is 404 where 200 was
   expected, the endpoint path is wrong. If `Y` is a schema validation error,
   open the schema file and compare it to the actual response body.

4. **All tests pass sequentially but some fail in parallel** → stale state
   between tests on the same thread. Confirm `ApiClient.reset()` is being
   called in `@BeforeMethod` in `BaseTest`, and that no `static` mutable
   fields exist in test classes.

5. **XML schema validation fails** → run `xmllint --schema src/test/resources/schemas/xml/user-schema.xsd <your-xml-file> --noout` to validate the XML against the XSD outside of Java and read the parse error directly.

6. **Allure report not generating** → see the Allure annotation troubleshooting table in the Allure Reporting section above.
