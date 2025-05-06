package com.spring.spring.dto.fournisseur;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PurchaseOrderResponseDTO {
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
        @JsonProperty("purchase_orders")
        private List<PurchaseOrder> purchaseOrders;
        private int count;
    }

    @Getter @Setter
    public static class PurchaseOrder {
        @JsonProperty("name")
        private String name;
        @JsonProperty("supplier")
        private String supplier;
        @JsonProperty("supplier_name")
        private String supplierName;
        @JsonProperty("grand_total")
        private Double grandTotal;
        @JsonProperty("transaction_date")
        private String transactionDate;
        @JsonProperty("status")
        private String status;
        @JsonProperty("is_paid")
        private Boolean isPaid;
        @JsonProperty("is_received")
        private Boolean isReceived;
        @JsonProperty("items")
        private List<Item> items;
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
    }
}
