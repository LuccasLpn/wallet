# 🟦 Pix Wallet Service — Code Assessment

Microserviço de carteira digital com suporte a Pix, garantindo **consistência**, **concorrência segura** e **idempotência**, seguindo **Clean Architecture**, com rastreabilidade completa via **ledger**.

---

## 📌 Índice

- [Visão Geral](#-visão-geral)
- [Arquitetura e Design](#-arquitetura-e-design)
- [Fluxos Principais](#-fluxos-principais)
- [Concorrência, Idempotência e Exatamente uma vez](#-concorrência-idempotência-e-exatamente-uma-vez)
- [Requisitos Atendidos](#-requisitos-atendidos)
- [Como Rodar o Projeto](#-como-rodar-o-projeto)
- [Testes](#-testes)
- [Decisões de Design](#-decisões-de-design)
- [Trade-offs](#-trade-offs)
- [Time Tracking](#time-tracking)

---

## 🧩 Visão Geral

Este serviço implementa:

*   Criação de carteiras
*   Registro de chaves Pix
*   Depósitos e saques
*   Transferências Pix internas (com `endToEndId`)
*   Webhook de confirmação / rejeição (lidando com eventos duplicados ou fora de ordem)
*   Cálculo de saldo (atual e histórico)
*   **Ledger imutável** com rastreabilidade total
*   **Idempotência** completa
*   **Consistência** mesmo sob concorrência

Toda a lógica segue **Clean Architecture**, separando domínio, casos de uso, adapters, controllers e infraestrutura.


# 📊 Observabilidade — Métricas Customizadas de Negócio

Este documento detalha o conjunto de métricas customizadas de negócio implementadas na aplicação, utilizando o **Micrometer** para instrumentação, expostas via **Spring Boot Actuator**, coletadas pelo **Prometheus** e visualizadas em *dashboards* do **Grafana**.

A instrumentação dessas métricas é fundamental para a **observabilidade** da plataforma, permitindo o acompanhamento do comportamento operacional, a detecção rápida de falhas e a criação de alarmes automáticos e proativos.

## 🚀 Endpoints de Métricas

A aplicação expõe os seguintes *endpoints* padrão do Spring Boot Actuator para acesso às métricas:

| Descrição | Endpoint |
| :--- | :--- |
| Lista todas as métricas disponíveis | `/actuator/metrics` |
| Detalhes de uma métrica específica | `/actuator/metrics/{nome}` |
| Endpoint para *scrape* do Prometheus | `/actuator/prometheus` |

## 📈 Métricas de Negócio

As métricas foram agrupadas por caso de uso (*use case*) e seguem um padrão de nomenclatura (`caso_de_uso.ação.tipo`) para facilitar a criação de *dashboards* e regras de alarme consistentes.

---

### 🟢 1. Criação de Carteira (*Wallet Creation*)

**Prefixo:** `wallet.create.*`

Este grupo de métricas monitora o processo de criação de novas carteiras na plataforma.

| Métrica | Tipo | Tags | Descrição |
| :--- | :--- | :--- | :--- |
| `wallet.create.total` | **Counter** | `result=success|error` | Quantidade total de tentativas de criação de carteiras, discriminada por resultado. |
| `wallet.create.duration.seconds` | **Timer** | `result=success|error` | Tempo de execução do processo de criação da carteira, discriminado por resultado. |

**O que monitorar:**

*   **Queda repentina** no número de criações (`wallet.create.total` com `result=success`), indicando um possível problema de fluxo ou integração.
*   **Aumento de latência** (`wallet.create.duration.seconds`), que pode sinalizar gargalos, como problemas de desempenho no banco de dados.

---

### 🟡 2. Depósitos (*Deposits*)

**Prefixo:** `wallet.deposit.*`

Métricas relacionadas à entrada de fundos na carteira do usuário.

| Métrica | Tipo | Tags | Descrição |
| :--- | :--- | :--- | :--- |
| `wallet.deposit.total` | **Counter** | `result` | Quantidade total de depósitos processados. |
| `wallet.deposit.amount.brl` | **Summary** | N/A | Volume financeiro total depositado (em BRL). |
| `wallet.deposit.duration.seconds` | **Timer** | N/A | Tempo de execução do processamento do depósito. |
| `wallet.deposit.errors.total` | **Counter** | `error_type` | Contagem de erros durante o processamento do depósito, categorizados pelo tipo de erro. |

**Alertas Recomendados:**

*   Depósitos com erro (`wallet.deposit.errors.total`) acima de um limite **X por minuto**.
*   Latência P99 (`wallet.deposit.duration.seconds`) acima do limite normal, indicando degradação da experiência do usuário.

---

### 🔴 3. Saques (*Withdrawals*)

**Prefixo:** `wallet.withdraw.*`

Métricas essenciais para monitorar a saída de fundos e a saúde financeira da operação.

| Métrica | Tipo | Tags | Descrição |
| :--- | :--- | :--- | :--- |
| `wallet.withdraw.total` | **Counter** | `result` | Quantidade total de saques solicitados, discriminada por resultado. |
| `wallet.withdraw.insufficient_funds.total` | **Counter** | N/A | Tentativas de saque rejeitadas por saldo insuficiente. |
| `wallet.withdraw.amount.brl` | **Summary** | N/A | Volume financeiro total sacado (em BRL). |
| `wallet.withdraw.errors.total` | **Counter** | N/A | Contagem de erros gerais no processamento de saques. |
| `wallet.withdraw.duration.seconds` | **Timer** | N/A | Tempo de execução do processamento do saque. |

**Importância:**

*   **Indicador de Problema para o Usuário Final:** Erros ou latência alta são indicadores claros de problemas na experiência do usuário.
*   **Análise de Comportamento:** A métrica `wallet.withdraw.insufficient_funds.total` é crucial para o time de produto entender o comportamento do usuário e ajustar limites ou regras de negócio.

---

### 🔵 4. Registro de Chaves Pix

**Prefixo:** `wallet.pix_key.*`

Métricas focadas no processo de registro e gestão de chaves Pix.

| Métrica | Tipo | Tags | Descrição |
| :--- | :--- | :--- | :--- |
| `wallet.pix_key.register.total` | **Counter** | `result`, `type` | Total de tentativas de registro de chave Pix, discriminado por resultado e tipo de chave (e.g., CPF, EMAIL, EV P). |
| `wallet.pix_key.already_in_use.total` | **Counter** | `type` | Contagem de tentativas de registro de chave que já está em uso, categorizada por tipo de chave. |
| `wallet.pix_key.register.errors.total` | **Counter** | `type`, `error_type` | Contagem de erros no registro, categorizados por tipo de chave e tipo de erro. |
| `wallet.pix_key.register.duration.seconds` | **Timer** | N/A | Tempo de execução do processo de registro da chave Pix. |

**Uso no Dashboard:**

*   Visualizar a **distribuição por tipo de chave** (`type`) para entender a preferência do usuário.
*   Monitorar a **taxa de chaves rejeitadas** (erros) para identificar problemas no fluxo ou na comunicação com o PSP (Provedor de Serviços de Pagamento).

---

### 🔶 5. Transferência Pix (*Pix Transfer*)

Este grupo de métricas abrange tanto a criação da transferência quanto o processamento via *webhooks*.

#### Criação da Transferência

| Métrica | Tipo | Tags | Descrição |
| :--- | :--- | :--- | :--- |
| `pix_transfer_requests_total` | **Counter** | N/A | Total de requisições de transferência Pix recebidas. |
| `pix_transfer_created_total` | **Counter** | N/A | Total de transferências Pix criadas com sucesso. |
| `pix_transfer_idempotency_hit_total` | **Counter** | N/A | Contagem de requisições que resultaram em acerto de idempotência (*hit*). |
| `pix_transfer_idempotency_miss_total` | **Counter** | N/A | Contagem de requisições que resultaram em falha de idempotência (*miss*). |
| `pix_transfer_insufficient_funds_total` | **Counter** | N/A | Tentativas de transferência rejeitadas por saldo insuficiente. |
| `pix_transfer_processing_seconds` | **Timer** | N/A | Latência total do processamento da transferência. |

**O que acompanhar:**

*   **Taxa de Idempotência:** Acompanhar o percentual de *hits* para garantir a robustez contra reenvios de requisição.
*   **Tentativas Inválidas:** Monitorar `pix_transfer_insufficient_funds_total` para entender o comportamento de uso e limites.
*   **Latência de Criação:** Garantir que o tempo de resposta para o usuário esteja dentro do SLA.

#### Webhooks

*   **Eventos Confirmados / Rejeitados:** Monitorar o fluxo de eventos de *webhook* para garantir que as atualizações de status (sucesso/falha) estejam sendo processadas corretamente.
*   **Latência do PSP → Aplicação:** Medir o tempo entre o evento no PSP e o processamento na aplicação.
*   **Volume Financeiro por Status:** Acompanhar o volume total de transferências por status final (concluída, falhada, etc.).


## 🏛️ Arquitetura e Design

A estrutura de diretórios reflete a Clean Architecture:

```
/src
├── domain
│    ├── model          → Entidades do domínio (Wallet, PixKey, PixTransfer, LedgerEntry…)
│    ├── enums          → Tipos da linguagem do domínio (PixTransferStatus etc.)
│    └── port           → Interfaces (repositories) consumidas pelos use cases
│
├── application
│    └── usecase        → Casos de uso (Deposit, Withdraw, PixTransfer…)
│
├── infrastructure
│    ├── persistence    → JPA entities + Spring Data repositories
│    └── config         → Beans dos use cases
│
└── interfaces
     └── rest           → Controllers + DTOs
```

**Benefícios:**

*   Fácil de testar (use cases são puros)
*   Baixo acoplamento
*   Domínio completamente isolado de JPA / HTTP
*   Infraestrutura substituível (ex.: trocar Postgres por DynamoDB sem tocar no domínio)

## 🔄 Fluxos Principais

| # | Fluxo | Endpoint | Descrição |
|---|---|---|---|
| 1 | **Criar carteira** | `POST /wallets` | Gera `walletId`, registra no banco, ledger inicial vazio, logs estruturados. |
| 2 | **Registrar chave Pix** | `POST /wallets/{id}/pix-keys` | Valida se wallet existe e a unicidade da chave. Salva chave do tipo EMAIL/PHONE/EVP. |
| 3 | **Depósito** | `POST /wallets/{id}/deposit` | Gera entrada de crédito imutável no ledger. Saldo é a soma das entradas. |
| 4 | **Saque** | `POST /wallets/{id}/withdraw` | Usa `findByIdForUpdate()` para bloqueio da wallet, valida saldo e cria entrada de débito. |
| 5 | **Transferência Pix (interna)** | `POST /pix/transfers` | Requer `Header: Idempotency-Key`. Cria `endToEndId`, gera débito na origem, cria `PixTransfer` com status `PENDING`. Retorna `{ endToEndId, status }`. |
| 6 | **Webhook Pix** | `POST /pix/webhook` | Salva evento (`eventId + endToEndId`), ignora duplicados, lida com ordem invertida. `CONFIRMED` → crédito na destino. `REJECTED` → reversão (crédito) na origem. |

## 🧵 Concorrência, Idempotência e Exatamente uma vez

| Recurso | Implementação | Benefício |
|---|---|---|
| **Debounce por Idempotency-Key** | Implementado em `IdempotentCreatePixTransferUseCase` e tabela `idempotency_records`. Garante que o mesmo request gere o mesmo response. | Idempotência em transferências. |
| **Lock pessimista para saldo** | Uso de `findByIdForUpdate()` em operações de débito. | Impede dois débitos simultâneos na mesma wallet, garantindo consistência. |
| **Eventual exactly-once no webhook** | Deduplicação por `eventId`. Status terminal (`CONFIRMED`/`REJECTED`) não muda mais. Ordem invertida é tratada corretamente. | Processamento seguro de eventos externos. |
| **Ledger imutável** | Nenhuma entrada é alterada ou removida. | Histórico 100% auditável e fonte única de verdade. |

## ✔ Requisitos Atendidos

| Requisito | Status | Onde |
|---|---|---|
| Criar carteira | ✅ | `CreateWalletUseCase` |
| Registro chave Pix | ✅ | `RegisterPixKeyUseCase` |
| Saldo atual | ✅ | `GetWalletBalanceUseCase.currentBalance()` |
| Saldo histórico | ✅ | `balanceAt()` |
| Depósito | ✅ | `DepositUseCase` |
| Saque | ✅ | `WithdrawUseCase` |
| Transferência Pix interna | ✅ | `CreatePixTransferUseCase` |
| Idempotência Pix | ✅ | `IdempotentCreatePixTransferUseCase` |
| Webhook Pix | ✅ | `HandlePixWebhookUseCase` |
| Deduplicação de eventos | ✅ | via `PixEventRepository.findByEventId()` |
| Ordem trocada | ✅ | máquina de estados simples + ledger |
| Concorrência | ✅ | lock pessimista + idempotência |
| Auditoria & Rastreabilidade | ⭐⭐⭐⭐⭐ | ledger imutável + logs estruturados |

## ▶️ Como Rodar o Projeto

1.  **Subir Postgres**
    ```bash
    docker compose up -d
    ```

2.  **Rodar a aplicação**
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **Endpoints principais**

    *   **Criar wallet**
        ```
        POST /wallets
        Body: { "ownerId": "random ou fixo" }
        ```
    *   **Registrar Pix Key**
        ```
        POST /wallets/{id}/pix-keys
        ```
    *   **Depósito**
        ```
        POST /wallets/{id}/deposit
        ```
    *   **Saque**
        ```
        POST /wallets/{id}/withdraw
        ```
    *   **Transferência Pix**
        ```
        POST /pix/transfers
        Header: Idempotency-Key: <uuid>
        ```
    *   **Webhook Pix**
        ```
        POST /pix/webhook
        ```

## 🧪 Testes

O projeto inclui:

*   Testes unitários dos use cases
*   Testes integrados (JPA + containers)
*   Mock de repositórios
*   Cenários de concorrência simulada

## 🧠 Decisões de Design

*   **Ledger como fonte da verdade:** Evita inconsistências, pois o saldo é derivado.
*   **Lock pessimista para operações críticas:** `findByIdForUpdate()` evita *race conditions*.
*   **Idempotência explícita via tabela:** Solução robusta para o caso clássico de duplicidade de requisições.
*   **PixEvent + PixTransfer = máquina de estados simples:** Suporta eventos fora de ordem, conforme o requisito.
*   **Clean Architecture:** Facilita testes, evoluções e substituição de infraestrutura.

# ⚖️ Trade-offs e Evoluções Futuras para o Sistema de Ledger

Este documento apresenta uma análise dos trade-offs de design atuais do sistema de ledger em tempo real e propõe evoluções futuras para otimizar performance, consistência e escalabilidade.

## 1. Ledger Calculado em Tempo Real

| Design Atual | Trade-off | Evolução Proposta |
| :--- | :--- | :--- |
| **Cálculo de Saldo em Tempo Real** (derivado do histórico de transações) | Simples e confiável, mas menos performático para consultas históricas. | Manter **snapshots periódicos de saldo** (por dia ou por número de transações) para reduzir o custo de consultas históricas, recalculando apenas o “rabo” recente do ledger. |

## 2. Estratégia de Concorrência

| Design Atual | Trade-off | Evolução Proposta |
| :--- | :--- | :--- |
| **Lock Pessimista** (na transação de banco de dados) | Seguro e garante consistência sob alta concorrência, porém reduz o paralelismo. | Evoluir para um modelo com menos contenção (ex.: **filas por partição de carteira**) e reduzir o escopo do lock apenas à atualização de saldo/ledger. |
| **Uso de Fila SQS FIFO para Comandos Críticos** | Não adotado. Aumenta a complexidade operacional (infraestrutura e monitoria). | Adotar SQS FIFO para: **garantir ordenação** das operações por carteira (`walletId` como chave de agrupamento) e **reduzir a chance de *race conditions*** antes de chegar ao banco. Suportar **idempotência** com *message group id* + *deduplication id*. |

## 3. Máquina de Estados e Resiliência

| Design Atual | Trade-off | Evolução Proposta |
| :--- | :--- | :--- |
| **Máquina de Estados Simplificada** (apenas PENDING, COMPLETED, FAILED) | Não inclui um estado `SETTLED`, focando apenas nos requisitos operacionais essenciais. | Adicionar um estado extra para **liquidação final (`SETTLED`)** para separar claramente: <ul><li>estados operacionais (PENDING, COMPLETED, FAILED)</li><li>estados contábeis/financeiros (SETTLED, REVERSED)</li></ul> |
| **Sistema de Retries** | Não implementado para falhas de DB. | Implementar um sistema de retries. Uma abordagem mais robusta seria combinar **retries com *outbox pattern*** e **entrega assíncrona via fila**, evitando reprocessar transações já aplicadas. |

## 4. Estratégia de Dados e Performance de Leitura

| Design Atual | Trade-off | Evolução Proposta |
| :--- | :--- | :--- |
| **Saldo Derivado Diretamente do Ledger** | Simples, mas a performance de leitura e relatórios é limitada pelo volume de dados do ledger. | Implementar uma **Estratégia mais Inteligente para o Ledger (Materialização Parcial / CQRS)**: <ul><li>Criar uma tabela de saldos materializados (`wallet_balance`) atualizada por eventos do ledger.</li><li>Usar **CQRS (Command Query Responsibility Segregation)**: `write model` (ledger imutável) e `read model` (visão otimizada para consultas).</li></ul> **Trade-off da Evolução:** Aumenta a complexidade de sincronização entre *write/read model*, mas melhora muito a performance de leitura e relatórios. |

## 5. Escalabilidade e Manutenção do Banco de Dados

| Design Atual | Trade-off | Evolução Proposta |
| :--- | :--- | :--- |
| **Tabela Única de Ledger** | O ledger tende a crescer bastante ao longo do tempo, impactando performance e custo de armazenamento. | Aplicar **Versionamento e Particionamento de Tabela** devido ao volume de dados: <ul><li>**Particionar** a tabela de ledger por período (ex.: mês/ano) ou por carteira.</li><li>**Mover registros antigos** para uma tabela de arquivo/histórico (`ledger_history`).</li><li>Aplicar **versionamento lógico** (ex.: `schema_v1`, `schema_v2`) para suportar mudanças de estrutura sem *downtime*.</li></ul> **Trade-off da Evolução:** O time precisa lidar com mais complexidade de migrações e *queries* multi-partição, mas ganha em performance, custo de armazenamento e flexibilidade de evolução do modelo de dados. |
