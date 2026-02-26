# B2C Module

Domínio financeiro para Pessoa Física.

---

## Responsabilidades

- Movimentações pessoais
- Categorias
- Relatórios
- Dashboard

---

## O que NÃO faz

- Não gerencia autenticação
- Não gerencia empresas (CNPJ)

---

## Dependência

- Identity (validação JWT)
- ImportData
- OpenFinance

---

## Porta padrão

8082

---

## Build isolado
./mvnw -pl b2c clean install