package br.com.finlumia.configurator.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.services.TableService;
import br.com.finlumia.shared.views.DialogDefault;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configurator")
@Tag(name = "Configurador de tabelas", description = "Endpoints para operações de tabelas no configurador.")
public class TableController {

    @Autowired
    private TableService tableService;

    @PostMapping("/tables")
    @Operation(summary = "Realiza o cadastro da tabela", description = "Realiza o cadastro da tabela no configurador.")
    @ApiResponse(responseCode = "200", description = "Tabela cadastrada com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autorizado")
    public DialogDefault createTable(
            @RequestAttribute("keyUser") Long keyUser,
            @Valid @RequestBody CreateTableRequest request) {
        return tableService.createTable(keyUser, request);
    }

    @PutMapping("/tables")
    @Operation(summary = "Realiza a atualizacao da tabela", description = "Realiza a atualizacao da tabela no configurador.")
    @ApiResponse(responseCode = "200", description = "Tabela atualizada com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autorizado")
    public DialogDefault updateTable(
            @RequestAttribute("keyUser") Long keyUser,
            @Valid @RequestBody UpdateTableRequest request) {
        return tableService.updateTable(keyUser, request);
    }

    @DeleteMapping("/tables")
    @Operation(summary = "Realiza a exclusao logica da tabela", description = "Realiza a exclusao logica da tabela no configurador.")
    @ApiResponse(responseCode = "200", description = "Tabela excluida com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autorizado")
    public DialogDefault deleteTable(
            @RequestAttribute("keyUser") Long keyUser,
            @Valid @RequestBody DeleteTableRequest request) {
        return tableService.deleteTable(keyUser, request);
    }

}
