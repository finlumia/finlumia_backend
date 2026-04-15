package br.com.finlumia.configurator.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.DeleteTableRequest;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.repository.TableRepository;
import br.com.finlumia.shared.views.DialogDefault;

@Service
public class TableService {
    @Autowired
    private TableRepository tableRepository;

    public DialogDefault createTable(Long keyUser, CreateTableRequest request) {
        return _createTable(keyUser, request);
    }

    public DialogDefault updateTable(Long keyUser, UpdateTableRequest request) {
        return _updateTable(keyUser, request);
    }

    public DialogDefault deleteTable(Long keyUser, DeleteTableRequest request) {
        return _deleteTable(keyUser, request);
    }


    private DialogDefault _createTable(Long keyUser, CreateTableRequest request) {
        return tableRepository.insertTable(keyUser, request);
    }

    private DialogDefault _updateTable(Long keyUser, UpdateTableRequest request) {
        return tableRepository.updateTable(keyUser, request);
    }
    
    private DialogDefault _deleteTable(Long keyUser, DeleteTableRequest request) {
        return tableRepository.deleteTable(keyUser, request);
    }
}
