package com.sta.dhbw.stauserver.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sta.dhbw.stauserver.util.Constants;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.Serializable;

/**
 * This class represents a registered user.
 */
@JsonIgnoreProperties({"userIdHash"})
public class UserResource implements Serializable
{
    private String userId;

    private String userIdHash;

    public UserResource()
    {
    }

    public UserResource(String userId, String userIdHash)
    {
        this.userId = userId;
        this.userIdHash = userIdHash;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getUserIdHash()
    {
        return userIdHash;
    }

    public void setUserIdHash(String userIdHash)
    {
        this.userIdHash = userIdHash;
    }

    public JsonObject toJsonObject()
    {
        return Json.createObjectBuilder()
                .add(Constants.USER_ID, getUserId()).build();
    }
}
