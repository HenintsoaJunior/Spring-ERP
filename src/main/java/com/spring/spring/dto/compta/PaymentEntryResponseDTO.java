package com.spring.spring.dto.compta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentEntryResponseDTO {
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
        @JsonProperty("payment_entry_name")
        private String paymentEntryName;

        @JsonProperty("invoice_name")
        private String invoiceName;

        @JsonProperty("amount_paid")
        private Double amountPaid;

        @JsonProperty("invoice_status")
        private String invoiceStatus;

        @JsonProperty("paid_from")
        private String paidFrom;

        @JsonProperty("paid_to")
        private String paidTo;

        @JsonProperty("payment_date")
        private String paymentDate;

        @JsonProperty("mode_of_payment")
        private String modeOfPayment;
    }
}