package com.spring.spring.dto.compta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PurchaseInvoiceResponseDTO {
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
        private List<PurchaseInvoice> invoices;
        private int count;
    }

    @Getter @Setter
    public static class PurchaseInvoice {
        @JsonProperty("name")
        private String name;

        @JsonProperty("status")
        private String status;

        @JsonProperty("supplier")
        private String supplier;

        @JsonProperty("posting_date")
        private String postingDate;

        @JsonProperty("grand_total")
        private Double grandTotal;

        @JsonProperty("outstanding_amount")
        private Double outstandingAmount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("items")
        private List<Item> items;

        @JsonProperty("item_count")
        private Integer itemCount;
    }

    @Getter @Setter
    public static class Item {
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

        @JsonProperty("purchase_order")
        private String purchaseOrder;

        @JsonProperty("purchase_receipt")
        private String purchaseReceipt;
    }
}