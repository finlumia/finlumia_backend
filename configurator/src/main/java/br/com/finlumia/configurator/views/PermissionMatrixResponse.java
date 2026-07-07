package br.com.finlumia.configurator.views;

import java.util.List;
import java.util.Map;

public record PermissionMatrixResponse(Map<String, List<PermissionResponse>> matrix) {
}
