package br.com.yann.sextafeira.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/v1/ping")
    public String ping() {
        return "SEXTA-FEIRA online";
    }

}
