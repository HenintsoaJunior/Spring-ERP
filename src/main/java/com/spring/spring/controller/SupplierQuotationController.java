package com.spring.spring.controller;

import com.spring.spring.dto.fournisseur.FournisseurResponseDTO;
import com.spring.spring.dto.fournisseur.SupplierQuotationResponseDTO;
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
@RequestMapping("/api/spring/fournisseur/quotation-devis")
public class SupplierQuotationController {
    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;

    private String GETFOURNISSEUR_URL;
    private String GETQUOTATIONS_URL;

    private String UPDATE_QUOTATION_ITEM_URL;

    @PostConstruct
    public void init() {
        GETFOURNISSEUR_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.fournisseur";
        GETQUOTATIONS_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.supplier_quotations";
        UPDATE_QUOTATION_ITEM_URL = erpnextApiUrl + "/api/method/erpnext.services.fournisseur.fournisseur_service.make_update_quotation_item";
    }


    @GetMapping("/supplier-quotation")
    public String getSupplierQuotation(Model model, HttpSession session) {
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

            ResponseEntity<FournisseurResponseDTO> fournisseurResponse = restTemplate.exchange(
                GETFOURNISSEUR_URL,
                HttpMethod.GET,
                entity,
                FournisseurResponseDTO.class
            );

            FournisseurResponseDTO fournisseurData = fournisseurResponse.getBody();
            if (fournisseurData != null && fournisseurData.getMessage() != null && fournisseurData.getMessage().getData() != null) {
                model.addAttribute("fournisseur", fournisseurData.getMessage().getData().getFournisseur());
                model.addAttribute("count", fournisseurData.getMessage().getData().getCount());
                model.addAttribute("message", fournisseurData.getMessage().getMessage());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des fournisseurs.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/devis";
    }

    @PostMapping("/supplier-quotation/filter")
    public String filterSupplierQuotations(@RequestParam("supplier") String supplier, Model model, HttpSession session) {
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

            ResponseEntity<FournisseurResponseDTO> fournisseurResponse = restTemplate.exchange(
                GETFOURNISSEUR_URL,
                HttpMethod.GET,
                entity,
                FournisseurResponseDTO.class
            );

            FournisseurResponseDTO fournisseurData = fournisseurResponse.getBody();
            if (fournisseurData != null && fournisseurData.getMessage() != null && fournisseurData.getMessage().getData() != null) {
                model.addAttribute("fournisseur", fournisseurData.getMessage().getData().getFournisseur());
                model.addAttribute("count", fournisseurData.getMessage().getData().getCount());
                model.addAttribute("message", fournisseurData.getMessage().getMessage());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des fournisseurs.");
            }

            String url = GETQUOTATIONS_URL;
            if (supplier != null && !supplier.isEmpty()) {
                url += "?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
            }
            System.out.println("Calling API: " + url);

            ResponseEntity<SupplierQuotationResponseDTO> quotationResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SupplierQuotationResponseDTO.class
            );
            System.out.println("API Response: " + quotationResponse.getBody());

            SupplierQuotationResponseDTO quotationData = quotationResponse.getBody();
            if (quotationData != null && quotationData.getMessage() != null && quotationData.getMessage().getData() != null) {
                model.addAttribute("quotations", quotationData.getMessage().getData().getQuotations());
                model.addAttribute("quotationCount", quotationData.getMessage().getData().getCount());
                model.addAttribute("quotationMessage", quotationData.getMessage().getMessage());
                model.addAttribute("quotationErrors", quotationData.getMessage().getErrors());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des devis.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/devis";
    }

    
    @GetMapping("/supplier-quotation/update-form")
    public String showUpdateRateForm(
        @RequestParam(value = "quotationName", required = false) String quotationName,
        @RequestParam(value = "itemCode", required = false) String itemCode,
        @RequestParam(value = "rate", required = false) Double rate,
        @RequestParam(value = "qty", required = false) Double qty,
        @RequestParam(value = "amount", required = false) Double amount,
        Model model
    ) {
        // Use flash attributes if query parameters are null
        model.addAttribute("quotationName", model.containsAttribute("quotationName") ? model.getAttribute("quotationName") : quotationName);
        model.addAttribute("itemCode", model.containsAttribute("itemCode") ? model.getAttribute("itemCode") : itemCode);
        model.addAttribute("rate", model.containsAttribute("rate") ? model.getAttribute("rate") : rate);
        model.addAttribute("qty", model.containsAttribute("qty") ? model.getAttribute("qty") : qty);
        model.addAttribute("amount", model.containsAttribute("amount") ? model.getAttribute("amount") : amount);
        return "pages/fournisseur/update_rate";
    }

    @PostMapping("/supplier-quotation/update")
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
                redirectAttributes.addFlashAttribute("quotationName", quotationName);
                redirectAttributes.addFlashAttribute("itemCode", itemCode);
                redirectAttributes.addFlashAttribute("rate", rate);
                redirectAttributes.addFlashAttribute("qty", 0.0); // Default value
                redirectAttributes.addFlashAttribute("amount", 0.0); // Default value
                return "redirect:/api/spring/fournisseur/quotation-devis/supplier-quotation/update-form";
            }

            if (quotationName == null || itemCode == null || rate == null) {
                redirectAttributes.addFlashAttribute("error", "Paramètres manquants pour la mise à jour.");
                redirectAttributes.addFlashAttribute("quotationName", quotationName);
                redirectAttributes.addFlashAttribute("itemCode", itemCode);
                redirectAttributes.addFlashAttribute("rate", rate);
                redirectAttributes.addFlashAttribute("qty", 0.0); // Default value
                redirectAttributes.addFlashAttribute("amount", 0.0); // Default value
                return "redirect:/api/spring/fournisseur/quotation-devis/supplier-quotation/update-form";
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
                return "redirect:/api/spring/fournisseur/quotation/request-quotation"; // Redirect to quotation list
            } else {
                redirectAttributes.addFlashAttribute("error", apiMessage);
                redirectAttributes.addFlashAttribute("quotationName", quotationName);
                redirectAttributes.addFlashAttribute("itemCode", itemCode);
                redirectAttributes.addFlashAttribute("rate", rate);
                redirectAttributes.addFlashAttribute("qty", 0.0); // Default value
                redirectAttributes.addFlashAttribute("amount", 0.0); // Default value
                return "redirect:/api/spring/fournisseur/quotation-devis/supplier-quotation/update-form";
            }

        } catch (Exception e) {
            String errorMessage = "Erreur lors de la mise à jour : " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMessage);
            redirectAttributes.addFlashAttribute("quotationName", quotationName);
            redirectAttributes.addFlashAttribute("itemCode", itemCode);
            redirectAttributes.addFlashAttribute("rate", rate);
            redirectAttributes.addFlashAttribute("qty", 0.0); // Default value
            redirectAttributes.addFlashAttribute("amount", 0.0); // Default value
            return "redirect:/api/spring/fournisseur/quotation-devis/supplier-quotation/update-form";
        }
    }
}