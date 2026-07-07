package br.com.finlumia.movement.controllers.external;

import java.util.List;
import java.util.Map;

import br.com.finlumia.movement.controllers.ExternalApi;
import br.com.finlumia.movement.services.CategoryService;
import br.com.finlumia.movement.views.CategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Categorias disponíveis para lançamentos")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorias", description = "Lista todas as categorias disponíveis com metadados de cor e ícone.")
    @ApiResponse(responseCode = "200", description = "Categorias retornadas com sucesso")
    public ResponseEntity<Map<String, List<CategoryView>>> list() {
        return ResponseEntity.ok(Map.of("data", categoryService.list()));
    }
}
