package com.laker.postman.request.model;

import java.util.List;
import java.util.Map;

// Chuyển hướng cấu trúc thông tin
public class RedirectInfo {
    public String url;
    public int statusCode;
    public Map<String, List<String>> headers;
    public String location;
    public String responseBody;
}