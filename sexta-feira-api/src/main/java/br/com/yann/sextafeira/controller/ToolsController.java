package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.dto.ConvertRequest;
import br.com.yann.sextafeira.dto.ConvertResponse;
import br.com.yann.sextafeira.service.CurrencyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolsController {

    private final CurrencyService currencyService;

    public ToolsController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @PostMapping("/convert")
    public ConvertResponse convert(@RequestBody ConvertRequest request) {
        return currencyService.converter(request);
    }
}
