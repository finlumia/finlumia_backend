package br.com.finlumia.configurator.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.services.TableService;
import br.com.finlumia.shared.views.DialogDefault;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configurator")
public class TableController {

    private static final String HEADER_KEY_USER = "X-Key-User";

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping("/tables")
    public DialogDefault createTable(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody CreateTableRequest request) {
        return tableService.createTable(keyUser, request);
    }

    @PutMapping("/tables")
    public DialogDefault updateTable(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody UpdateTableRequest request) {
        return tableService.updateTable(keyUser, request);
    }

    @DeleteMapping("/tables")
    public DialogDefault deleteTable(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody DeleteTableRequest request) {
        return tableService.deleteTable(keyUser, request);
    }

    
}
