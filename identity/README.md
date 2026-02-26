# Identity Module

Responsável por autenticação e autorização da plataforma.

Não contém regra de negócio financeira.

---

## Responsabilidades

- Cadastro de usuário
- Login
- Emissão de JWT
- Validação de JWT
- Resolução de tenant (CPF / CNPJ)

---

## O que NÃO faz

- Não contém regras B2C ou B2B
- Não contém lógica financeira

---

## Porta padrão

8081

---

## Executar isoladamente
./mvnw -pl identity spring-boot:run


---

## Dependências externas

- PostgreSQL
- Spring Security