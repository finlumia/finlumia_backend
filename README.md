# Finlumia

> Plataforma web de análise de finanças pessoais baseada na importação e processamento de extratos financeiros.

---

## Sobre o Projeto

O Finlumia permite que usuários importem arquivos de extratos financeiros ou realizem lançamentos manuais de movimentações, possibilitando o processamento, organização e visualização das transações de forma estruturada, clara e visual por meio de painéis gráficos.

A plataforma oferece funcionalidades de categorização de transações e filtros de análise para que o usuário compreenda seus padrões de gastos e mantenha o controle de suas finanças pessoais.

---

## Problema que Resolve

- Dificuldade em organizar e compreender finanças pessoais
- Falta de controle sobre despesas recorrentes e uso de crédito
- Limitação na identificação das áreas de maior impacto financeiro (alimentação, moradia, saúde, etc.)
- Ausência de uma visão consolidada e estruturada dos hábitos de consumo

---

## Benefícios

- Centralização das informações financeiras em um único ambiente
- Melhor compreensão dos padrões de consumo
- Identificação das categorias com maior impacto financeiro
- Apoio na tomada de decisões financeiras conscientes
- Redução do risco de endividamento descontrolado
- Visualização clara por meio de painéis gráficos

---

## Público-Alvo

- Pessoas físicas que desejam organizar e controlar suas finanças pessoais
- Usuários com renda fixa ou variável com dificuldades na gestão de despesas
- Indivíduos que utilizam múltiplas fontes de pagamento (cartão de crédito, débito, transferências)
- Usuários com baixo ou médio nível de conhecimento em educação financeira

---

## Módulos do Sistema (visão de produto)

### Configurador

Permite que administradores e desenvolvedores configurem dinamicamente o sistema sem necessidade de deploy. Inclui criação de tabelas, campos, menus, parâmetros globais, triggers, functions, índices, regras visuais e condicionais.

### Identidade

Gerencia identidade, acesso e ciclo de vida do usuário na plataforma. Inclui cadastro, login, recuperação de senha, gestão de perfil, assinatura e exclusão de conta.

### Movimentações

Permite ao usuário registrar e gerenciar suas transações financeiras com suporte a categorias personalizadas, filtros, movimentações recorrentes e saldo consolidado.

### Analítico

Transforma dados financeiros em insights visuais com dashboards, gráficos (pizza, linha, barra), KPIs, filtros dinâmicos, comparação de períodos e exportação de relatórios.

### Pagamento

Gerencia planos, assinaturas e integração com gateway de pagamento (Stripe ou Mercado Pago) com suporte a webhooks e notificações ao usuário.

### Documentação

Disponibiliza conhecimento e suporte para uso da plataforma, tanto para usuários finais quanto para desenvolvedores.

---

## Este repositório: `finlumia_backend`

Monorepo **Gradle** com serviços **Spring Boot** em Java. Neste repositório estão, hoje, os módulos de backend abaixo; outros módulos de produto (analítico, pagamento, etc.) podem residir em outros repositórios ou ser incorporados futuramente.

| Módulo Gradle        | Papel |
|----------------------|--------|
| `shared-config`      | Biblioteca compartilhada: Spring Web MVC, Security, OAuth2 Client, JDBC, validação, OpenAPI (Springdoc), exceções e DTOs comuns (ex.: `DialogDefault`). |
| `configurator`       | API do configurador: metadados de tabelas e campos no PostgreSQL, geração física de `CREATE TABLE` / `ALTER TABLE ... ADD COLUMN` a partir do configurador. |
| `identity`           | Serviço de identidade (sobre `shared-config`). |
| `movement`           | Serviço de movimentações (sobre `shared-config`). |

**Stack principal:** Java 25, Spring Boot 4.0.x, PostgreSQL (driver JDBC), Lombok.

---

## Arquitetura do backend

### Organização em camadas

Nos módulos de aplicação (por exemplo `configurator`), o código segue separação em:

1. **Controllers** — REST, validação de entrada (`jakarta.validation`), cabeçalhos (ex.: `X-Key-User`), respostas HTTP com `DialogDefault` quando aplicável.
2. **Services** — regras de negócio, orquestração e montagem de DDL dinâmico onde necessário.
3. **Repositories** — acesso a dados via `DataSource` (JDBC), `PreparedStatement`, transações quando exigido.
4. **SQL estático** — classes `*Sql` em `repository.sql` concentram strings de consulta e fragmentos de DDL reutilizáveis; comandos que dependem de colunas geradas dinamicamente permanecem montados no serviço.

### Configurador: fluxo de metadados e banco físico

- Metadados de **tabelas** e **campos** persistidos em esquema `configurator` no PostgreSQL.
- Endpoints REST sob prefixo `/api/configurator` (tabelas, campos, sincronização física).
- **Criação física de tabelas:** serviço lê tabelas pendentes, monta `CREATE SCHEMA` / `CREATE TABLE` / índices conforme campos configurados e atualiza flags no configurador.
- **Criação física de colunas:** para tabelas já materializadas, o serviço compara o catálogo (`pg_catalog`) com os campos do configurador e executa `ALTER TABLE ... ADD COLUMN` (e índices quando configurado).

### Dados e segurança

- Conexão JDBC qualificada (`postgresDataSource`) para o PostgreSQL de aplicação.
- Spring Security e OAuth2 Client disponíveis via `shared-config` (detalhes de proteção de endpoints por módulo evoluem com o projeto).

### Diretrizes alinhadas ao produto

- Arquitetura modular com separação entre regras de negócio e configuração.
- Configuração dinâmica de entidades (tabelas, campos, parâmetros) com caminho para evolução sem redeploy completo da modelagem de dados.
- Estrutura preparada para versionamento e evolução incremental.
- Suporte conceitual a menus configuráveis e hierárquicos.
- Preparado para pipeline de dados futuro (Airflow / PySpark), conforme roadmap.

---

## Modelo de Negócio

| Plano | Preço | Recursos |
|-------|--------|----------|
| **Free** | Gratuito | Importação de até 3 extratos, até 200 movimentações manuais, dashboard básico |
| **Pro** | R$ 19–29/mês | Extratos e movimentações ilimitados, insights automáticos, relatórios |
| **Premium** | Em breve | IA financeira e previsões |

---

## Conformidade Legal

- **LGPD** — Consentimento, direito à exclusão e portabilidade de dados
- **Código de Defesa do Consumidor** — Transparência na cobrança e cancelamento facilitado
- **Marco Civil da Internet** — Registro de logs
- **GDPR** — Preparado para expansão internacional

---

## Métricas de Negócio

- **MRR** — Receita Recorrente Mensal
- **Churn** — Taxa de cancelamento
- **LTV** — Lifetime Value
- **CAC** — Custo de Aquisição de Cliente

---

## Desenvolvimento local

Pré-requisitos: JDK 25, Gradle Wrapper (incluído no repositório), PostgreSQL acessível conforme configuração do ambiente.

```bash
# compilar todos os módulos
./gradlew build

# exemplo: compilar apenas o configurador
./gradlew :configurator:compileJava
```

Cada módulo `configurator`, `identity` e `movement` é uma aplicação Spring Boot independente; variáveis de conexão e perfis devem ser definidos em `application` properties ou variáveis de ambiente do seu ambiente.

Rodar o projeto localmente

./gradlew :configurator:bootRun