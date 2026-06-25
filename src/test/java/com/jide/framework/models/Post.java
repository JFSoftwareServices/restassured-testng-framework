package com.jide.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Post represents the post resource returned by the API.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) means if the API returns fields
 * not declared here, Jackson silently ignores them rather than throwing an
 * error. This is the correct default for test frameworks — you declare only
 * the fields you care about asserting.
 *
 * @JacksonXmlRootElement(localName = "post") tells Jackson to use <user> as
 * the root XML element when serialising this POJO to XML, and to expect
 * <user> as the root element when deserialising XML responses.
 *
 * The same POJO is used for both JSON and XML because Jackson's ObjectMapper
 * (JSON) and XmlMapper (XML) share the same field mapping — you just call
 * the appropriate mapper for the format.
 */
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