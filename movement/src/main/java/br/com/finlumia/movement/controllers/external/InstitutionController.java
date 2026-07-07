package br.com.finlumia.movement.controllers.external;

import java.util.List;
import java.util.Map;

import br.com.finlumia.movement.controllers.ExternalApi;
import br.com.finlumia.movement.services.InstitutionService;
import br.com.finlumia.movement.views.InstitutionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/v1/institutions")
@Tag(name = "Institutions", description = "Instituições financeiras suportadas")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    @Operation(summary = "Listar instituições", description = "Lista instituições financeiras suportadas com cores e abreviações.")
    @ApiResponse(responseCode = "200", description = "Instituições retornadas com sucesso")
    public ResponseEntity<Map<String, List<InstitutionView>>> list() {
        return ResponseEntity.ok(Map.of("data", institutionService.list()));
    }
}
