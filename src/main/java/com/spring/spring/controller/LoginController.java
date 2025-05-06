package com.spring.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {

    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;

    private String LOGIN_ENDPOINT;
    private String LOGOUT_ENDPOINT;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @PostConstruct
    public void init() {
        LOGIN_ENDPOINT = erpnextApiUrl + "/api/method/erpnext.controllers.api.login.login";
        LOGOUT_ENDPOINT = erpnextApiUrl + "/api/method/logout";
    }
    @GetMapping("/")
    public String showLoginPage(Model model) {
        return "pages/loginAdminPage";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "pages/dashboard";
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/api/spring/login")
    public String login(@RequestParam("email") String email, 
                       @RequestParam("password") String password, 
                       HttpSession session, 
                       Model model) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String formData = "usr=" + email + "&pwd=" + password;
            HttpEntity<String> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                LOGIN_ENDPOINT,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> responseMap = mapper.readValue(response.getBody(), Map.class);
                    
                    if (responseMap.containsKey("message") && responseMap.get("message") instanceof Map) {
                        Map<String, Object> messageMap = (Map<String, Object>) responseMap.get("message");
                        
                        if (messageMap.containsKey("sid")) {
                            String sid = (String) messageMap.get("sid");
                            
                            session.setAttribute("access_token", sid);
                            
                            if (messageMap.containsKey("api_key")) {
                                session.setAttribute("api_key", messageMap.get("api_key"));
                            }
                            
                            if (messageMap.containsKey("api_secret")) {
                                session.setAttribute("api_secret", messageMap.get("api_secret"));
                            }
                            
                            if (messageMap.containsKey("username")) {
                                session.setAttribute("username", messageMap.get("username"));
                            }
                            
                            if (responseMap.containsKey("full_name")) {
                                session.setAttribute("full_name", responseMap.get("full_name"));
                            }
                            
                            logger.info("Le token dans la session est : " + sid);
                            return "redirect:/dashboard";
                        }
                    }
                    
                    model.addAttribute("errorMessage", "Login Error");
                    return "pages/loginAdminPage";
                    
                } catch (Exception e) {
                    logger.error("Error parsing JSON response: " + e.getMessage());
                    model.addAttribute("errorMessage", "Error processing server response: " + e.getMessage());
                    return "pages/loginAdminPage";
                }
            } else {
                model.addAttribute("errorMessage", "Invalid credentials");
                return "pages/loginAdminPage";
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Connection: " + e.getMessage());
            return "pages/loginAdminPage";
        }
    }

    @GetMapping("/api/spring/logout")
    public String logout(HttpSession session) {
        try {
            String sid = (String) session.getAttribute("access_token");
            if (sid != null) {
                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.set("Cookie", "sid=" + sid);

                HttpEntity<String> request = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    LOGOUT_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Logout successful for SID: " + sid);
                } else {
                    logger.warn("Logout failed for SID: " + sid + ", response: " + response.getBody());
                }
            } else {
                logger.warn("No SID found in session for logout");
            }

        } catch (Exception e) {
            logger.error("Error during logout: " + e.getMessage());
        }

        // Clear all session attributes
        session.invalidate();
        
        return "redirect:/";
    }
}