package com.spring.spring.controller;

import com.spring.spring.dto.compta.PurchaseInvoiceResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.spring.spring.dto.compta.PaymentEntryResponseDTO;
import com.spring.spring.dto.fournisseur.FournisseurResponseDTO;
import com.spring.spring.utils.ErpnextAuthUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/spring/compta")
public class ComptaController {

    private static final Logger logger = LoggerFactory.getLogger(ComptaController.class);
    
    @Value("${erpnext.api.url}")
    private String erpnextApiUrl;
    
    private String GET_FOURNISSEUR_URL;
    private String GET_PURCHASE_INVOICES_URL;
    private String CREATE_PAYMENT_ENTRY_URL;

    @PostConstruct
    public void init() {
        GET_FOURNISSEUR_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.fournisseur";
        GET_PURCHASE_INVOICES_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.purchase_invoices";
        CREATE_PAYMENT_ENTRY_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.payment_entry";
    }

    private final RestTemplate restTemplate;

    public ComptaController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/invoices/export")
    public String exportInvoiceToPdf(
            @RequestParam(value = "invoiceName", required = false) String invoiceName,
            @RequestParam(value = "supplier", required = false) String supplier,
            HttpServletResponse response,
            HttpSession session) {
        
        Logger logger = LoggerFactory.getLogger(ComptaController.class);
        
        try {
            // Configuration de la réponse HTTP pour le téléchargement du PDF
            response.setContentType("application/pdf");
            String fileName = "facture_" + (invoiceName != null ? invoiceName : "liste") + "_" + 
                            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            
            // Créer un document PDF
            Document document = new Document();
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            
            // Styles des polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            // En-tête du document
            Paragraph title = new Paragraph("Facture d'achat", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);
            
            // Création des headers pour authentification
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            
            if (!headers.containsKey("Authorization")) {
                document.add(new Paragraph("Erreur: Clés API manquantes. Veuillez vous connecter.", normalFont));
                document.close();
                return null;
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            String GET_PURCHASE_INVOICES_URL = erpnextApiUrl + "/api/method/erpnext.controllers.api.rest_api.purchase_invoices";
            
            // Construction de l'URL avec les paramètres de filtre
            StringBuilder urlBuilder = new StringBuilder(GET_PURCHASE_INVOICES_URL);
            boolean hasParam = false;
            
            if (invoiceName != null && !invoiceName.isEmpty()) {
                urlBuilder.append("?invoice_name=").append(URLEncoder.encode(invoiceName, StandardCharsets.UTF_8));
                hasParam = true;
            }
            
            if (supplier != null && !supplier.isEmpty()) {
                urlBuilder.append(hasParam ? "&" : "?").append("supplier=").append(URLEncoder.encode(supplier, StandardCharsets.UTF_8));
            }
            
            // Appel API pour récupérer les données de facture
            ResponseEntity<PurchaseInvoiceResponseDTO> invoiceResponse = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                entity,
                PurchaseInvoiceResponseDTO.class
            );
            
            PurchaseInvoiceResponseDTO invoiceData = invoiceResponse.getBody();
            
            if (invoiceData != null && invoiceData.getMessage() != null && 
                invoiceData.getMessage().getData() != null && 
                invoiceData.getMessage().getData().getInvoices() != null) {
                
                var invoices = invoiceData.getMessage().getData().getInvoices();
                
                if (invoices.isEmpty()) {
                    document.add(new Paragraph("Aucune facture trouvée avec les critères spécifiés.", normalFont));
                } else {
                    // Pour chaque facture trouvée
                    for (var invoice : invoices) {
                        // Informations principales de la facture
                        document.add(new Paragraph("Numéro de facture: " + invoice.getName(), headerFont));
                        document.add(new Paragraph("Fournisseur: " + invoice.getSupplier(), normalFont));
                        document.add(new Paragraph("Date: " + invoice.getPostingDate(), normalFont));
                        document.add(new Paragraph("Montant total: " + invoice.getGrandTotal() + " " + invoice.getCurrency(), normalFont));
                        document.add(new Paragraph("Montant restant: " + invoice.getOutstandingAmount() + " " + invoice.getCurrency(), normalFont));
                        document.add(new Paragraph("Statut: " + invoice.getStatus(), normalFont));
                        document.add(new Paragraph("\n"));
                        
                        // Tableau des articles
                        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                            document.add(new Paragraph("Articles de la facture:", headerFont));
                            
                            PdfPTable table = new PdfPTable(5);
                            table.setWidthPercentage(100);
                            
                            // En-têtes du tableau
                            String[] headers1 = {"Article", "Quantité", "Montant", "Bon de commande", "Réception"};
                            for (String header : headers1) {
                                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                cell.setPadding(5);
                                table.addCell(cell);
                            }
                            
                            // Contenu du tableau
                            for (var item : invoice.getItems()) {
                                table.addCell(new Phrase(item.getItemName(), normalFont));
                                table.addCell(new Phrase(String.valueOf(item.getQty()), normalFont));
                                table.addCell(new Phrase(item.getAmount() + " " + invoice.getCurrency(), normalFont));
                                table.addCell(new Phrase(item.getPurchaseOrder() != null ? item.getPurchaseOrder() : "-", normalFont));
                                table.addCell(new Phrase(item.getPurchaseReceipt() != null ? item.getPurchaseReceipt() : "-", normalFont));
                            }
                            
                            document.add(table);
                        } else {
                            document.add(new Paragraph("Aucun article disponible pour cette facture.", normalFont));
                        }
                        
                        // Ajouter un saut de page entre les factures
                        if (invoices.indexOf(invoice) < invoices.size() - 1) {
                            document.newPage();
                        }
                    }
                }
            } else {
                document.add(new Paragraph("Erreur: Aucune donnée de facture disponible.", normalFont));
            }
            
            // Pied de page avec date d'exportation
            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Document généré le " + 
                            new SimpleDateFormat("dd/MM/yyyy à HH:mm:ss").format(new Date()), normalFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);
            
            document.close();
            return null;  // Pas besoin de retourner une vue car nous retournons directement le PDF
            
        } catch (DocumentException | IOException e) {
            logger.error("Erreur lors de la génération du PDF", e);
            return "redirect:/api/spring/compta/invoices?error=Erreur+lors+de+la+génération+du+PDF:+" + e.getMessage();
        } catch (Exception e) {
            logger.error("Erreur inattendue", e);
            return "redirect:/api/spring/compta/invoices?error=Erreur+inattendue:+" + e.getMessage();
        }
    }

    @GetMapping("/invoices")
    public String getPurchaseInvoices(Model model, HttpSession session) {
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
                GET_FOURNISSEUR_URL,
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

            // Récupérer les factures d'achat
            ResponseEntity<PurchaseInvoiceResponseDTO> invoiceResponse = restTemplate.exchange(
                GET_PURCHASE_INVOICES_URL,
                HttpMethod.GET,
                entity,
                PurchaseInvoiceResponseDTO.class
            );

            PurchaseInvoiceResponseDTO invoiceData = invoiceResponse.getBody();
            if (invoiceData != null && invoiceData.getMessage() != null && invoiceData.getMessage().getData() != null) {
                model.addAttribute("invoices", invoiceData.getMessage().getData().getInvoices());
                model.addAttribute("invoiceCount", invoiceData.getMessage().getData().getCount());
                model.addAttribute("invoiceMessage", invoiceData.getMessage().getMessage());
                model.addAttribute("invoiceErrors", invoiceData.getMessage().getErrors());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des factures d'achat.");
            }

        } catch (Exception e) {
            logger.error("Error fetching invoices or suppliers", e);
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/compta/purchase_invoices";
    }

    @PostMapping("/invoices/filter")
    public String filterPurchaseInvoices(@RequestParam("supplier") String supplier, Model model, HttpSession session) {
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
                GET_FOURNISSEUR_URL,
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

            // Récupérer les factures d'achat avec filtre fournisseur
            String url = GET_PURCHASE_INVOICES_URL;
            if (supplier != null && !supplier.isEmpty()) {
                url += "?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
            }

            ResponseEntity<PurchaseInvoiceResponseDTO> invoiceResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                PurchaseInvoiceResponseDTO.class
            );

            PurchaseInvoiceResponseDTO invoiceData = invoiceResponse.getBody();
            if (invoiceData != null && invoiceData.getMessage() != null && invoiceData.getMessage().getData() != null) {
                model.addAttribute("invoices", invoiceData.getMessage().getData().getInvoices());
                model.addAttribute("invoiceCount", invoiceData.getMessage().getData().getCount());
                model.addAttribute("invoiceMessage", invoiceData.getMessage().getMessage());
                model.addAttribute("invoiceErrors", invoiceData.getMessage().getErrors());
            } else {
                model.addAttribute("error", "Aucune donnée reçue de l'API des factures d'achat.");
            }

        } catch (Exception e) {
            logger.error("Error filtering invoices", e);
            model.addAttribute("error", "Erreur lors de la récupération des données : " + e.getMessage());
        }

        return "pages/compta/purchase_invoices";
    }

    @PostMapping("/invoices/payment")
    public String createPaymentEntry(@RequestParam("invoiceName") String invoiceName, 
                                    @RequestParam(value = "paidAmount", required = false) Double paidAmount, 
                                    Model model, HttpSession session) {
        try {
            // Create headers with authorization
            HttpHeaders headers = ErpnextAuthUtil.createAuthorizationHeader(session);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (!headers.containsKey("Authorization")) {
                logger.warn("Missing API keys in session");
                model.addAttribute("error", "Clés API manquantes. Veuillez vous connecter.");
                return "pages/loginAdminPage";
            }

            // Create request body for payment entry
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("invoice_name", invoiceName);
            if (paidAmount != null && paidAmount > 0) {
                requestBodyMap.put("paid_amount", paidAmount);
            }
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Call the create_payment_entry API
            try {
                ResponseEntity<PaymentEntryResponseDTO> paymentResponse = restTemplate.exchange(
                    CREATE_PAYMENT_ENTRY_URL,
                    HttpMethod.POST,
                    entity,
                    PaymentEntryResponseDTO.class
                );

                PaymentEntryResponseDTO paymentData = paymentResponse.getBody();
                if (paymentResponse.getStatusCode().is2xxSuccessful() && paymentData != null && paymentData.getMessage() != null) {
                    if ("success".equals(paymentData.getMessage().getStatus())) {
                        model.addAttribute("paymentMessage", paymentData.getMessage().getMessage());
                        model.addAttribute("paymentData", paymentData.getMessage().getData());
                    } else {
                        logger.error("Payment API returned non-success status: {}", paymentData.getMessage().getMessage());
                        model.addAttribute("error", "Erreur de l'API de paiement : " + paymentData.getMessage().getMessage());
                    }
                } else {
                    logger.error("Invalid or empty response from payment API: {}", paymentResponse.getBody());
                    model.addAttribute("error", "Réponse invalide ou vide de l'API de paiement.");
                }
            } catch (HttpClientErrorException e) {
                logger.error("HTTP error calling payment API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                model.addAttribute("error", "Erreur HTTP lors de l'appel à l'API de paiement : " + e.getResponseBodyAsString());
            } catch (Exception e) {
                logger.error("Unexpected error calling payment API", e);
                model.addAttribute("error", "Erreur inattendue lors de l'appel à l'API de paiement : " + e.getMessage());
            }

            // Fetch updated invoices
            HttpEntity<String> invoiceEntity = new HttpEntity<>(headers);
            try {
                ResponseEntity<PurchaseInvoiceResponseDTO> invoiceResponse = restTemplate.exchange(
                    GET_PURCHASE_INVOICES_URL,
                    HttpMethod.GET,
                    invoiceEntity,
                    PurchaseInvoiceResponseDTO.class
                );

                PurchaseInvoiceResponseDTO invoiceData = invoiceResponse.getBody();
                if (invoiceResponse.getStatusCode().is2xxSuccessful() && invoiceData != null && 
                    invoiceData.getMessage() != null && invoiceData.getMessage().getData() != null) {
                    model.addAttribute("invoices", invoiceData.getMessage().getData().getInvoices());
                    model.addAttribute("invoiceCount", invoiceData.getMessage().getData().getCount());
                    model.addAttribute("invoiceMessage", invoiceData.getMessage().getMessage());
                    model.addAttribute("invoiceErrors", invoiceData.getMessage().getErrors());
                } else {
                    logger.error("Invalid or empty response from invoices API: {}", invoiceResponse.getBody());
                    model.addAttribute("error", "Aucune donnée reçue de l'API des factures d'achat.");
                }
            } catch (Exception e) {
                logger.error("Error fetching invoices", e);
                model.addAttribute("error", "Erreur lors de la récupération des factures : " + e.getMessage());
            }

            // Fetch supplier data
            try {
                ResponseEntity<FournisseurResponseDTO> fournisseurResponse = restTemplate.exchange(
                    GET_FOURNISSEUR_URL,
                    HttpMethod.GET,
                    invoiceEntity,
                    FournisseurResponseDTO.class
                );

                FournisseurResponseDTO fournisseurData = fournisseurResponse.getBody();
                if (fournisseurResponse.getStatusCode().is2xxSuccessful() && fournisseurData != null && 
                    fournisseurData.getMessage() != null && fournisseurData.getMessage().getData() != null) {
                    model.addAttribute("fournisseur", fournisseurData.getMessage().getData().getFournisseur());
                    model.addAttribute("count", fournisseurData.getMessage().getData().getCount());
                    model.addAttribute("message", fournisseurData.getMessage().getMessage());
                } else {
                    logger.error("Invalid or empty response from suppliers API: {}", fournisseurResponse.getBody());
                    model.addAttribute("error", "Aucune donnée reçue de l'API des fournisseurs.");
                }
            } catch (Exception e) {
                logger.error("Error fetching suppliers", e);
                model.addAttribute("error", "Erreur lors de la récupération des fournisseurs : " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("General error in createPaymentEntry", e);
            model.addAttribute("error", "Erreur générale lors de la création du paiement : " + e.getMessage());
        }

        return "pages/compta/purchase_invoices";
    }
}