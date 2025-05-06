package com.spring.spring.controller;

import com.spring.spring.dto.customer.CustomerResponseDTO;
import com.spring.spring.utils.ErpnextAuthUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;

@Controller
@RequestMapping("/api/spring")
public class CustomerController {

    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;

    private String API_URL;

    @PostConstruct
    public void init() {
        API_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.data";
    }

    @GetMapping("/customers")
    public String getCustomers(Model model, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);

            if (!headers.containsKey("Authorization")) {
                model.addAttribute("error", "Clés API manquantes. Veuillez vous connecter.");
                return "pages/loginAdminPage";
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerResponseDTO> response = restTemplate.exchange(
                API_URL,
                HttpMethod.GET,
                entity,
                CustomerResponseDTO.class
            );

            CustomerResponseDTO customerResponse = response.getBody();
            if (customerResponse != null && customerResponse.getMessage() != null && customerResponse.getMessage().getData() != null) {
                model.addAttribute("customers", customerResponse.getMessage().getData().getCustomers());
                model.addAttribute("count", customerResponse.getMessage().getData().getCount());
                model.addAttribute("message", customerResponse.getMessage().getMessage());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/customers/list";
    }
}
