package com.spring.spring.utils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;

public class ErpnextAuthUtil {

    public static HttpHeaders createAuthorizationHeader(HttpSession session) {
        String apiKey = (String) session.getAttribute("api_key");
        String apiSecret = (String) session.getAttribute("api_secret");

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && apiSecret != null) {
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
        }
        return headers;
    }
}