package com.spring.spring.dto.compta.erp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEntryERPResponseDTO {
    private MessageData message;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData {
        private String owner;
        private int docstatus;
        private int idx;
        private String namingSeries;
        private String paymentType;
        private String paymentOrderStatus;
        private String postingDate;
        private String company;
        private String partyType;
        private String party;
        private String partyName;
        private String bankAccount;
        private String paidFrom;
        private String paidFromAccountType;
        private String paidFromAccountCurrency;
        private String paidTo;
        private String paidToAccountType;
        private String paidToAccountCurrency;
        private double paidAmount;
        private double paidAmountAfterTax;
        private double sourceExchangeRate;
        private double basePaidAmount;
        private double basePaidAmountAfterTax;
        private double receivedAmount;
        private double receivedAmountAfterTax;
        private double targetExchangeRate;
        private double baseReceivedAmount;
        private double baseReceivedAmountAfterTax;
        private double totalAllocatedAmount;
        private double baseTotalAllocatedAmount;
        private double unallocatedAmount;
        private double differenceAmount;
        private int applyTaxWithholdingAmount;
        private double baseTotalTaxesAndCharges;
        private double totalTaxesAndCharges;
        private String referenceDate;
        private String costCenter;
        private String status;
        private int customRemarks;
        private String isOpening;
        private String bank;
        private String doctype;
        private List<PaymentReference> references;
        private List<Object> taxes;
        private List<Object> deductions;
        private int islocal;
        private int __unsaved;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentReference {
        private int docstatus;
        private int idx;
        private String referenceDoctype;
        private String referenceName;
        private String dueDate;
        private double paymentTermOutstanding;
        private double totalAmount;
        private double outstandingAmount;
        private double allocatedAmount;
        private double exchangeRate;
        private double exchangeGainLoss;
        private double paymentRequestOutstanding;
        private String parentfield;
        private String parenttype;
        private String doctype;
        private int islocal;
    }
}