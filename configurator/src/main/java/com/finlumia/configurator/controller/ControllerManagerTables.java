package com.finlumia.configurator.controller;

import com.finlumia.configurator.model.ModelDeleteTable;
import com.finlumia.configurator.model.ModelInsertTable;
import com.finlumia.configurator.model.ModelUpdateTable;
import com.finlumia.configurator.service.ServiceManagerTables;
import com.finlumia.configurator.view.ResponseDialog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configurator")
@Tag(name = "Configurator - Manager Tables", description = "Endpoints referente ao gerenciador de tabelas do configurador.")
public class ControllerManagerTables {

    @Autowired
    private ServiceManagerTables ServiceManagerTables;

    @GetMapping("/insert_table")
    @Operation(summary = "Inserir Tabela", description = "Realiza o insert na tabela system_table")
    public ResponseDialog InsertTable(ModelInsertTable modelInsertTable){
        return  new ResponseDialog();
    }

    @GetMapping("/update_table")
    @Operation(summary = "Atualizar Tabela", description = "Atualiza um registro na tabela system_table")
    public ResponseDialog UpdateTable(ModelUpdateTable modelUpdateTable){
        return  new ResponseDialog();
    }

    @GetMapping("/delete_table")
    @Operation(summary = "Delete Tabela", description = "Deleta um registro na tabela system_table")
    public ResponseDialog DeleteTabel(@Valid @RequestBody ModelDeleteTable modelDeleteTable){
        return  new ResponseDialog();
    }



}
