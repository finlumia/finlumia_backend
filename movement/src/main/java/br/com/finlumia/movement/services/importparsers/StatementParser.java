package br.com.finlumia.movement.services.importparsers;

public interface StatementParser {
    ParseResult parse(byte[] fileContent);
}
