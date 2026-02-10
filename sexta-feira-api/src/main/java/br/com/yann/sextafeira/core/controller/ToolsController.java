package br.com.yann.sextafeira.core.controller;

import br.com.yann.sextafeira.core.dto.ConvertRequest;
import br.com.yann.sextafeira.core.dto.ConvertResponse;
import br.com.yann.sextafeira.modes.finance.service.CurrencyService;
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
