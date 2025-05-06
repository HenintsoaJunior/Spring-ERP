package com.spring.spring.dto.compta.erp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResultDTO {
    private MessageData message;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData {
        private String doctype;
        private String name;
        private boolean saved;
        private boolean submitted;
    }
}