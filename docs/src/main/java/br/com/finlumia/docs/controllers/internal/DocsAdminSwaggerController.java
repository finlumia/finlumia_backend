package br.com.finlumia.docs.controllers.internal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs/admin")
public class DocsAdminSwaggerController {

    @GetMapping("/swagger-ui.html")
    public String adminSwaggerUi() {
        return "redirect:/docs/swagger-ui.html?configUrl=/docs/admin/swagger-config.json";
    }
}
