package br.com.finlumia.configurator.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.finlumia.configurator.models.CreateFieldRequest;
import br.com.finlumia.configurator.models.DeleteFieldRequest;
import br.com.finlumia.configurator.models.UpdateFieldRequest;
import br.com.finlumia.configurator.services.FieldService;
import br.com.finlumia.shared.views.DialogDefault;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configurator")
public class FieldControler {

    private static final String HEADER_KEY_USER = "X-Key-User";

    private final FieldService fieldService;

    public FieldControler(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @PostMapping("/fields")
    public ResponseEntity<DialogDefault> createField(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody CreateFieldRequest request) {
        DialogDefault response = fieldService.createField(keyUser, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PutMapping("/fields")
    public ResponseEntity<DialogDefault> updateField(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody UpdateFieldRequest request) {
        DialogDefault response = fieldService.updateField(keyUser, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @DeleteMapping("/fields")
    public ResponseEntity<DialogDefault> deleteField(
            @RequestHeader(HEADER_KEY_USER) Long keyUser,
            @Valid @RequestBody DeleteFieldRequest request) {
        DialogDefault response = fieldService.deleteField(keyUser, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/fields/physical")
    public ResponseEntity<DialogDefault> syncPhysicalFields(
            @RequestHeader(HEADER_KEY_USER) Long keyUser) {
        DialogDefault response = fieldService.syncPhysicalFieldsFromConfigurator(keyUser);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
