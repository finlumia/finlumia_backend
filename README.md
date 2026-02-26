# Finlumia Backend

Finlumia é uma plataforma de gerenciamento e monitoramento financeiro.

A arquitetura foi construída como multi-module Maven com serviços independentes, permitindo evolução isolada, deploy separado e escalabilidade progressiva.

A plataforma atende dois domínios principais:

- B2C → Pessoa Física (CPF)
- B2B → Pessoa Jurídica (CNPJ)

---

# 1. Arquitetura

Arquitetura baseada em módulos independentes organizados por domínio (Bounded Context).

Módulos atuais:

- identity → autenticação e controle de acesso
- b2c → domínio financeiro para pessoa física
- b2b → domínio financeiro para pessoa jurídica
- configurator → parâmetros e customização
- importdata → importação de extratos
- openfinance → integração externa
- chatbot → interface conversacional
- social → domínio social (futuro)

Cada módulo é uma aplicação Spring Boot executável.

---

# 2. Decisões Arquiteturais

- Multi-module Maven para isolamento de build
- JWT centralizado via identity
- PostgreSQL como banco relacional principal
- Comunicação futura via eventos (evolução planejada)
- Separação explícita entre B2C e B2B para evitar regras condicionais complexas

---

# 3. Pré-requisitos

- Java 17
- Docker (opcional)
- PostgreSQL
- Linux ou MacOS recomendado

---

# 4. Build Completo

Na raiz:

./mvnw clean install


Isso compila todos os módulos.

---

# 5. Build de um único módulo

Sem quebrar o restante:
./mvnw -pl b2c clean install


Ou compilando dependências necessárias:


./mvnw -pl b2c -am clean install


`-pl` → project list  
`-am` → also make dependencies

---

# 6. Executar um único módulo


./mvnw -pl identity spring-boot:run


Ou via jar:


java -jar identity/target/identity-1.0.0.jar


---

# 7. Executar múltiplos módulos simultaneamente

Cada módulo deve ter porta própria definida em:


application.yml


Exemplo:

- identity → 8081
- b2c → 8082
- b2b → 8083

---

# 8. Banco de Dados

Estratégia atual:

- PostgreSQL único
- Separação lógica por schema
- Isolamento por tenant_id

Migração futura possível para banco por serviço.

---

# 9. Segurança

- Autenticação centralizada
- JWT
- Controle multi-tenant
- Permissões por perfil (planejado para B2B)

---

# 10. Observabilidade (Planejado)

- Logs estruturados
- Health endpoints
- Métricas via Actuator

---

# 11. Como adicionar um novo módulo

1. Criar pasta
2. Criar pom.xml herdando do finlumia-parent
3. Adicionar no <modules> do pom pai
4. Criar classe @SpringBootApplication
5. Rodar build

---

# 12. Estratégia de Evolução

Curto prazo:
- Modular monolith executável

Médio prazo:
- Comunicação assíncrona

Longo prazo:
- Extração para microservices reais