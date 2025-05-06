package com.spring.spring.controller;

import com.spring.spring.dto.fournisseur.FournisseurResponseDTO;
import com.spring.spring.dto.fournisseur.PurchaseOrderResponseDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/api/spring/fournisseur")
public class PurchaseOrderController {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderController.class);

    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;

    private String GETFOURNISSEUR_URL;
    private String GETPURCHASEORDERS_URL;
   

    @PostConstruct
    public void init() {
        GETFOURNISSEUR_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.fournisseur";
        GETPURCHASEORDERS_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.purchase_orders";
    }

    @PostMapping("/commande/filter")
    public String filterPurchaseOrders(
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "status_filter", required = false) String statusFilter,
            Model model,
            HttpSession session) {
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

            // Récupérer les données des fournisseurs
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

            // Récupérer les commandes d'achat
            StringBuilder url = new StringBuilder(GETPURCHASEORDERS_URL);
            boolean hasQuery = false;

            if (supplier != null && !supplier.isEmpty()) {
                url.append(hasQuery ? "&" : "?").append("supplier=").append(URLEncoder.encode(supplier, StandardCharsets.UTF_8));
                hasQuery = true;
            }

            if (statusFilter != null && !statusFilter.isEmpty()) {
                url.append(hasQuery ? "&" : "?").append("status_filter=").append(URLEncoder.encode(statusFilter, StandardCharsets.UTF_8));
            }

            System.out.println("Calling API: " + url);

            ResponseEntity<PurchaseOrderResponseDTO> purchaseOrderResponse = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    PurchaseOrderResponseDTO.class
            );

            PurchaseOrderResponseDTO purchaseOrderData = purchaseOrderResponse.getBody();
            if (purchaseOrderData != null && purchaseOrderData.getMessage() != null && purchaseOrderData.getMessage().getData() != null) {
                model.addAttribute("purchaseOrders", purchaseOrderData.getMessage().getData().getPurchaseOrders());
                model.addAttribute("purchaseOrderCount", purchaseOrderData.getMessage().getData().getCount());
                model.addAttribute("purchaseOrderMessage", purchaseOrderData.getMessage().getMessage());
                model.addAttribute("purchaseOrderErrors", purchaseOrderData.getMessage().getErrors());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des commandes d'achat.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/purchase_orders";
    }


    @GetMapping("/commande")
    public String getPurchaseOrder(Model model, HttpSession session) {
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

            // Fetch supplier data
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



            // Fetch purchase orders without filters
            ResponseEntity<PurchaseOrderResponseDTO> purchaseOrderResponse = restTemplate.exchange(
                    GETPURCHASEORDERS_URL,
                    HttpMethod.GET,
                    entity,
                    PurchaseOrderResponseDTO.class
            );

            PurchaseOrderResponseDTO purchaseOrderData = purchaseOrderResponse.getBody();

            if (purchaseOrderData != null && purchaseOrderData.getMessage() != null && purchaseOrderData.getMessage().getData() != null) {
                model.addAttribute("purchaseOrders", purchaseOrderData.getMessage().getData().getPurchaseOrders());
                model.addAttribute("purchaseOrderCount", purchaseOrderData.getMessage().getData().getCount());
                model.addAttribute("purchaseOrderMessage", purchaseOrderData.getMessage().getMessage());
                model.addAttribute("purchaseOrderErrors", purchaseOrderData.getMessage().getErrors());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des commandes d'achat.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/fournisseur/purchase_orders";
    }
}