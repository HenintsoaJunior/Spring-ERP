package com.spring.spring.dto.fournisseur;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RequestQuotationResponseDTO {
    private Message message;

    @Getter @Setter
    public static class Message {
        private String status;
        private Data data;
        private String message;
        private Object errors;
    }

    @Getter @Setter
    public static class Data {
        @JsonProperty("quotations")
        private List<Quotation> quotations;
        private int count;
    }

    @Getter @Setter
    public static class Quotation {
        @JsonProperty("name")
        private String name;
        @JsonProperty("transaction_date")
        private String transactionDate;
        @JsonProperty("status")
        private String status;
        @JsonProperty("items")
        private List<Item> items;
        @JsonProperty("suppliers")
        private List<Supplier> suppliers;
        @JsonProperty("supplier_quotations")
        private List<SupplierQuotation> supplierQuotations;
        @JsonProperty("supplier_quotation_items")
        private List<SupplierQuotationItem> supplierQuotationItems;
    }

    @Getter @Setter
    public static class Item {
        @JsonProperty("item_code")
        private String itemCode;
        @JsonProperty("item_name")
        private String itemName;
        @JsonProperty("qty")
        private Double qty;
    }

    @Getter @Setter
    public static class Supplier {
        @JsonProperty("supplier")
        private String supplier;
        @JsonProperty("supplier_name")
        private String supplierName;
    }

    @Getter @Setter
    public static class SupplierQuotation {
        @JsonProperty("name")
        private String name;
        @JsonProperty("supplier")
        private String supplier;
        @JsonProperty("supplier_name")
        private String supplierName;
        @JsonProperty("transaction_date")
        private String transactionDate;
        @JsonProperty("grand_total")
        private Double grandTotal;
        @JsonProperty("status")
        private String status;
        @JsonProperty("items")
        private List<SupplierQuotationItem> items;
    }

    @Getter
    @Setter
    public static class SupplierQuotationItem {
        @JsonProperty("item_code")
        private String itemCode;
        @JsonProperty("item_name")
        private String itemName;
        @JsonProperty("qty")
        private Double qty;
        @JsonProperty("rate")
        private Double rate;
        @JsonProperty("amount")
        private Double amount;
    }
}