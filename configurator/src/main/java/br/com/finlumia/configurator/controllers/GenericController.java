package br.com.finlumia.configurator.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finlumia.configurator.models.GenericListRequest;
import br.com.finlumia.configurator.models.GenericRegisterRequest;
import br.com.finlumia.configurator.services.GenericRegisterService;
import br.com.finlumia.configurator.views.GenericListResponse;
import br.com.finlumia.shared.views.DialogDefault;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configurator")
@Tag(name = "Configurador de genericos", description = "Endpoints para operações de genericos no configurador.")            
public class GenericController {

    @Autowired
    private GenericRegisterService genericRegisterService;
        
    @PostMapping("/generic_register")
    @Operation(summary = "Realiza o cadastro da tabela", description = "Realiza o cadastro da tabela no configurador.")
    @ApiResponse(responseCode = "200", description = "Tabela cadastrada com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autorizado")
    @ApiResponse(responseCode = "404", description = "Tabela não encontrada")
    @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    public DialogDefault createGenericLine(
            @RequestAttribute("keyUser") Long keyUser,
            @Valid @RequestBody GenericRegisterRequest genericRegisterRequest) {
        return genericRegisterService.createGenericLine(keyUser, genericRegisterRequest);
    }

    @PostMapping("/generic_list")
    @Operation(summary = "Realiza a listagem genérica", description = "Realiza a listagem genérica de uma tabela no configurador.")
    @ApiResponse(responseCode = "200", description = "Listagem genérica realizada com sucesso")
    @ApiResponse(responseCode = "401", description = "Não autorizado")
    @ApiResponse(responseCode = "404", description = "Listagem genérica não encontrada para a tabela informada")
    @ApiResponse(responseCode = "500", description = "Erro interno do servidor ao realizar a listagem genérica") 
    public GenericListResponse listGeneric(
            @RequestAttribute("keyUser") Long keyUser,
            @Valid @RequestBody GenericListRequest genericRegisterRequest) {
        return genericRegisterService.listGeneric(keyUser, genericRegisterRequest);
    }

    

    

    
}
