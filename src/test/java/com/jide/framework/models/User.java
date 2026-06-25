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