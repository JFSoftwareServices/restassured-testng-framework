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