package br.com.finlumia.configurator.services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.finlumia.configurator.models.GenericListRequest;
import br.com.finlumia.configurator.models.GenericRegisterRequest;
import br.com.finlumia.configurator.services.GenericRegisterRepository.DataTable;
import br.com.finlumia.configurator.services.GenericRegisterRepository.Field;
import br.com.finlumia.configurator.views.GenericListResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;

@Service
public class GenericRegisterService {

    @Autowired
    private GenericRegisterRepository genericRegisterRepository;

    public DialogDefault createGenericLine(Long keyUser, GenericRegisterRequest request) {
        return _createGenericLine(keyUser, request);
    }

    public DialogDefault updateGenericLine(Long keyUser, GenericRegisterRequest request) {
        return _updateGenericLine(keyUser, request);
    }

    public DialogDefault deleteGenericLine(Long keyUser, GenericRegisterRequest request) {
        return _deleteGenericLine(keyUser, request);
    }

    public GenericListResponse listGeneric(Long keyUser, GenericListRequest request) {
        return _getGenericList(keyUser, request);
    }

    private GenericListResponse _getGenericList(Long keyUser, GenericListRequest request) {

        DataTable dataTable = genericRegisterRepository.getGenericDataTable(keyUser, request.getSlugTable());

        // Montar campos da tabela para realizar o insert
        List<String> listFields = new ArrayList<>();
        StringJoiner selectFieldsJoiner = new StringJoiner(",");
        for (Field field : dataTable.fields.values()) {
            listFields.add(field.fieldName);
            selectFieldsJoiner.add(field.fieldName);
        }
        String selectFields = selectFieldsJoiner.toString();

        return genericRegisterRepository.getGenericItemTable(keyUser, dataTable, listFields, selectFields, request.getPage());

    }

    private DialogDefault _createGenericLine(Long keyUser, GenericRegisterRequest request) {

        DataTable dataTable = genericRegisterRepository.getGenericDataTable(keyUser, request.getSlugTable());

        // Associa o map do request com o map da tabela
        Map<Long, String> fields = request.getFields();
        for (Map.Entry<Long, String> entry : fields.entrySet()) {
            Long orderRequest = entry.getKey();
            String valueRequest = entry.getValue();
            Field fieldDataTable = dataTable.fields.get(orderRequest);
            if (fieldDataTable != null) {
                fieldDataTable.fieldParameter = valueRequest;
            } else {
                throw new FinlumiaException(400, "Campo não encontrado", "Campo não encontrado na tabela");
            }
        }

        // Realizar tratativa do tipo dos dados da tabela e realizar a conversao do
        // campo do request

        // Montar campos da tabela para realizar o insert
        StringBuilder insertFieldsBuilder = new StringBuilder("(");
        for (Field field : dataTable.fields.values()) {
            insertFieldsBuilder.append(field.fieldName).append(",");
        }
        String insertFields = insertFieldsBuilder.substring(0, insertFieldsBuilder.length() - 1) + ")";

        // Montar valores com conversao conforme o tipo da coluna
        StringBuilder insertValuesBuilder = new StringBuilder("(");
        for (Field field : dataTable.fields.values()) {
            insertValuesBuilder
                    .append(formatValueByFieldType(field.fieldType, field.fieldParameter, field.fieldName))
                    .append(",");
        }
        String insertValues = insertValuesBuilder.substring(0, insertValuesBuilder.length() - 1) + ")";

        // Inserir linha na tabela
        boolean insertSuccess = genericRegisterRepository.insertGenericLine(dataTable, insertFields, insertValues);
        if (insertSuccess) {
            return new DialogDefault(201, "Insert realizado com sucesso", "Linha inserida com sucesso");
        } else {
            throw new FinlumiaException(500, "Erro ao realizar o insert na tabela",
                    "Erro ao realizar o insert na tabela");
        }

    }

    private String formatValueByFieldType(String fieldType, String fieldValue, String fieldName) {
        String normalizedType = fieldType == null ? "" : fieldType.trim().toLowerCase();
        String normalizedValue = fieldValue.trim();
        try {
            switch (normalizedType) {
                case "varchar":
                case "text":
                case "char":
                case "timestamptz":
                    return "'" + normalizedValue.replace("'", "''") + "'";
                case "integer":
                    Integer.parseInt(normalizedValue);
                    return normalizedValue;
                case "bigint":
                    new BigInteger(normalizedValue);
                    return normalizedValue;
                case "numeric":
                    new BigDecimal(normalizedValue);
                    return normalizedValue;
                case "boolean":
                    if ("true".equalsIgnoreCase(normalizedValue) || "false".equalsIgnoreCase(normalizedValue)) {
                        return normalizedValue.toLowerCase();
                    }
                    throw new FinlumiaException(
                            400,
                            "Tipo inválido para campo boolean",
                            "Campo [" + fieldName + "] deve receber true ou false");
                case "jsonb":
                    return "'" + normalizedValue.replace("'", "''") + "'::jsonb";
                default:
                    throw new FinlumiaException(
                            400,
                            "Tipo de campo não suportado",
                            "Tipo [" + fieldType + "] do campo [" + fieldName + "] não suportado para insert");
            }
        } catch (NumberFormatException e) {
            throw new FinlumiaException(
                    400,
                    "Valor inválido para o tipo da coluna",
                    "Campo [" + fieldName + "] possui valor inválido [" + fieldValue + "] para tipo [" + fieldType
                            + "]");
        }
    }

    private DialogDefault _updateGenericLine(Long keyUser, GenericRegisterRequest request) {
        return new DialogDefault(null, null, null);
    }

    private DialogDefault _deleteGenericLine(Long keyUser, GenericRegisterRequest request) {
        return new DialogDefault(null, null, null);
    }

}
