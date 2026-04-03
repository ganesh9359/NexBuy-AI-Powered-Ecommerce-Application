package com.nexbuy.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppEntryController {

    private final String frontendUrl;

    public AppEntryController(@Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public String redirectToFrontend() {
        return "redirect:" + frontendUrl;
    }
}
