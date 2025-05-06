package com.spring.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class ImportController {

    @GetMapping("/import")
    public String showImportPage(HttpSession session, Model model) {
        String apiKey = (String) session.getAttribute("api_key");
        String apiSecret = (String) session.getAttribute("api_secret");

        model.addAttribute("apiKey", apiKey);
        model.addAttribute("apiSecret", apiSecret);

        return "pages/data/importdata";
    }

}
