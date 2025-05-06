package com.spring.spring.dto.customer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CustomerResponseDTO {
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
        private List<Customer> customers;
        private int count;
    }
    @Getter @Setter
    public static class Customer {
        @JsonProperty("name")
        private String name;

        @JsonProperty("email_id")
        private String emailId;

        @JsonProperty("owner")
        private String owner;

        @JsonProperty("customer_name")
        private String customerName;

        @JsonProperty("customer_type")
        private String customerType;
    }
}