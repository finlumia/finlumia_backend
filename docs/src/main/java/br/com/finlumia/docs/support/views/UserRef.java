package br.com.finlumia.docs.support.views;

import java.util.UUID;

public record UserRef(UUID id, String name, String email) {
}
