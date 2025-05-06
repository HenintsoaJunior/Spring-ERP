package com.spring.spring.controller;

import com.spring.spring.dto.fournisseur.FournisseurResponseDTO;
import com.spring.spring.dto.fournisseur.RequestQuotationResponseDTO;
import com.spring.spring.utils.ErpnextAuthUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
@RequestMapping("/api/spring/fournisseur/quotation")
public class RequestQuotationController {
    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;

    private String GETFOURNISSEUR_URL;
    private String REQUESTQUOTATIONS_URL;
    
    private String UPDATE_QUOTATION_ITEM_URL;
    
    @PostConstruct
    public void init() {
        GETFOURNISSEUR_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.fournisseur";
        REQUESTQUOTATIONS_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.request_quotations";
        UPDATE_QUOTATION_ITEM_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.update_request_quotation";
    }


    @GetMapping("/request-quotation")
    public String getRequestQuotation(Model model, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            if (!headers.containsKey("Authorization")) {
                model.addAttribute("error", "Clés API manquantes. Veuillez vous connecter.");
                return "pages/loginAdminPage";
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Récupération de tous les fournisseurs pour le filtre
            ResponseEntity<FournisseurResponseDTO> fournisseurResponse = restTemplate.exchange(
                GETFOURNISSEUR_URL,
                HttpMethod.GET,
                entity,
                FournisseurResponseDTO.class
            );

            FournisseurResponseDTO fournisseurData = fournisseurResponse.getBody();
            if (fournisseurData != null && fournisseurData.getMessage() != null && fournisseurData.getMessage().getData() != null) {
                model.addAttribute("fournisseurs", fournisseurData.getMessage().getData().getFournisseur());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des fournisseurs.");
            }

            // Récupération des demandes de devis
            ResponseEntity<RequestQuotationResponseDTO> quotationResponse = restTemplate.exchange(
                REQUESTQUOTATIONS_URL,
                HttpMethod.GET,
                entity,
                RequestQuotationResponseDTO.class
            );

            RequestQuotationResponseDTO quotationData = quotationResponse.getBody();
            if (quotationData != null && quotationData.getMessage() != null && quotationData.getMessage().getData() != null) {
                model.addAttribute("quotations", quotationData.getMessage().getData().getQuotations());
                model.addAttribute("count", quotationData.getMessage().getData().getCount());
                model.addAttribute("message", quotationData.getMessage().getMessage());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des demandes de devis.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/request_quotation";
    }

    @PostMapping("/request-quotation/filter")
    public String filterRequestQuotations(@RequestParam("supplier") String supplier, Model model, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            if (!headers.containsKey("Authorization")) {
                model.addAttribute("error", "Clés API manquantes. Veuillez vous connecter.");
                return "pages/loginAdminPage";
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Récupération de tous les fournisseurs pour le filtre (même en mode filtré)
            ResponseEntity<FournisseurResponseDTO> fournisseurResponse = restTemplate.exchange(
                GETFOURNISSEUR_URL,
                HttpMethod.GET,
                entity,
                FournisseurResponseDTO.class
            );

            FournisseurResponseDTO fournisseurData = fournisseurResponse.getBody();
            if (fournisseurData != null && fournisseurData.getMessage() != null && fournisseurData.getMessage().getData() != null) {
                model.addAttribute("fournisseurs", fournisseurData.getMessage().getData().getFournisseur());
            }

            // Construction de l'URL avec le paramètre de filtrage si nécessaire
            String url = REQUESTQUOTATIONS_URL;
            if (supplier != null && !supplier.isEmpty()) {
                url += "?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
            }

            // Appel à l'API avec le filtre
            ResponseEntity<RequestQuotationResponseDTO> quotationResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                RequestQuotationResponseDTO.class
            );

            RequestQuotationResponseDTO quotationData = quotationResponse.getBody();
            if (quotationData != null && quotationData.getMessage() != null && quotationData.getMessage().getData() != null) {
                model.addAttribute("quotations", quotationData.getMessage().getData().getQuotations());
                model.addAttribute("count", quotationData.getMessage().getData().getCount());
                model.addAttribute("message", quotationData.getMessage().getMessage());
                model.addAttribute("selectedSupplier", supplier); // Conserver le filtre sélectionné
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des demandes de devis.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/request_quotation";
    }

    @GetMapping("/request-quotation/update-form")
    public String showUpdateRateForm(
        @RequestParam("quotationName") String quotationName,
        @RequestParam("itemCode") String itemCode,
        @RequestParam("qty") Double qty,
        Model model
    ) {
        model.addAttribute("quotationName", quotationName);
        model.addAttribute("itemCode", itemCode);
        model.addAttribute("qty", qty);
        return "pages/fournisseur/update_request_quotation";
    }

    @PostMapping("/request-quotation/update")
    public String updateRate(
        @RequestParam("quotationName") String quotationName,
        @RequestParam("itemCode") String itemCode,
        @RequestParam("rate") Double rate,
        RedirectAttributes redirectAttributes,
        HttpSession session
    ) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.set("Content-Type", "application/json");

            if (!headers.containsKey("Authorization")) {
                redirectAttributes.addFlashAttribute("error", "Clés API manquantes. Veuillez vous connecter.");
                // Utilisation des redirectAttributes au lieu de rediriger avec des paramètres URL
                redirectAttributes.addFlashAttribute("quotationName", quotationName);
                redirectAttributes.addFlashAttribute("itemCode", itemCode);
                redirectAttributes.addFlashAttribute("rate", rate);
                // Spécifier les paramètres requis dans l'URL pour le GET
                return "redirect:/api/spring/fournisseur/quotation/request-quotation/update-form?quotationName=" + quotationName + 
                       "&itemCode=" + itemCode + 
                       "&rate=" + rate + 
                       "&qty=0&amount=0";
            }

            if (quotationName == null || itemCode == null || rate == null) {
                redirectAttributes.addFlashAttribute("error", "Paramètres manquants pour la mise à jour.");
                // Spécifier les paramètres requis dans l'URL pour le GET
                return "redirect:/api/spring/fournisseur/quotation/request-quotation/update-form?quotationName=" + quotationName + 
                       "&itemCode=" + itemCode + 
                       "&rate=" + rate + 
                       "&qty=0&amount=0";
            }

            var jsonObject = new java.util.HashMap<String, Object>();
            jsonObject.put("quotation_name", quotationName);
            jsonObject.put("item_code", itemCode);
            jsonObject.put("rate", rate);
            String requestBody = mapper.writeValueAsString(jsonObject);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                UPDATE_QUOTATION_ITEM_URL,
                HttpMethod.POST,
                entity,
                String.class
            );

            JsonNode responseJson = mapper.readTree(response.getBody());
            JsonNode messageNode = responseJson.get("message");
            String status = messageNode.get("status").asText();
            String apiMessage = messageNode.get("message").asText();

            if (response.getStatusCode().is2xxSuccessful() && status.equals("success")) {
                redirectAttributes.addFlashAttribute("successMessage", apiMessage);
                return "redirect:/api/spring/fournisseur/quotation/request-quotation";
            } else {
                redirectAttributes.addFlashAttribute("error", apiMessage);
                // Spécifier les paramètres requis dans l'URL pour le GET
                return "redirect:/api/spring/fournisseur/quotation/request-quotation/update-form?quotationName=" + quotationName + 
                       "&itemCode=" + itemCode + 
                       "&rate=" + rate + 
                       "&qty=0&amount=0";
            }

        } catch (Exception e) {
            String errorMessage = "Erreur lors de la mise à jour : " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMessage);
            // Spécifier les paramètres requis dans l'URL pour le GET
            return "redirect:/api/spring/fournisseur/quotation/request-quotation/update-form?quotationName=" + quotationName + 
                   "&itemCode=" + itemCode + 
                   "&rate=" + rate + 
                   "&qty=0&amount=0";
        }
    }
}