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