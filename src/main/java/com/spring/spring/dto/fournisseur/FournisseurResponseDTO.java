package com.spring.spring.dto.fournisseur;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FournisseurResponseDTO {
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
        @JsonProperty("suppliers")
        private List<Fournisseur> fournisseur;
        private int count;
    }

    @Getter @Setter
    public static class Fournisseur {
        @JsonProperty("name")
        private String name;
        @JsonProperty("email_id")
        private String emailId;
        @JsonProperty("owner")
        private String owner;
        @JsonProperty("supplier_name")
        private String supplierName;
        @JsonProperty("supplier_type")
        private String supplierType;
    }
}