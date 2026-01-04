# Testes CDC (Consumer Driven Contract) para lms_books_command

## Resumo
Foram criados testes CDC completos para o módulo `lms_books_command` baseados no padrão encontrado no módulo `lms_readers_command`. Os testes cobrem tanto o papel de consumidor quanto de produtor de eventos.

### ✅ TODOS OS EVENTOS IDENTIFICADOS E COBERTOS

Após análise completa do código fonte, foram identificados e implementados testes para **todos os 10 eventos** do módulo:

**EVENTOS CONSUMIDOS:**
1. **book_created** - Book criado por outro serviço
2. **book_updated** - Book atualizado por outro serviço  
3. **author_pending_created** - Author pendente criado (SAGA)
4. **genre_pending_created** - Genre pendente criado (SAGA)
5. **author_creation_failed** - Falha na criação de author (SAGA)
6. **genre_creation_failed** - Falha na criação de genre (SAGA)
7. **author_created** - Author criado
8. **genre_created** - Genre criado
9. **book_finalized** - Book finalizado (SAGA)
10. **lending_validation_request** - Request de validação de lending

**EVENTOS PRODUZIDOS:**
1. **BOOK_CREATED** - Book criado
2. **BOOK_UPDATED** - Book atualizado
3. **BOOK_DELETED** - Book deletado
4. **book_requested** - Book solicitado (SAGA)
5. **book_finalized** - Book finalizado (SAGA)
6. **lending_validation_response** - Resposta de validação de lending

## Estrutura de Testes

### 1. Testes de Definição de Contratos (Consumer)
**Arquivo:** `BooksCDCDefinitionTest.java`
- Define contratos para 10 tipos de eventos consumidos
- Gera arquivos JSON de contratos na pasta `target/pacts/`

### 2. Testes de Integração do Consumidor
**Arquivo:** `BooksCDCConsumerIT.java`
- Testa o processamento de mensagens baseado nos contratos gerados
- Verifica se o `BookRabbitmqController` processa corretamente as mensagens
- Usa mocks adequados para todas as dependências

### 3. Testes do Produtor (Provider)
**Arquivos:** 
- `BooksProducerCDCIT.java` - Testes usando arquivos locais
- `BooksProducerFromPactBrokerCDCIT.java` - Testes usando Pact Broker

Verificam se o serviço consegue produzir mensagens que atendem aos contratos definidos.

### 4. Utilitários
**Arquivos:**
- `BookMessageBuilder.java` - Constrói mensagens de teste
- `CDCTestConfiguration.java` - Configuração específica para testes CDC

## Como Executar os Testes

### 1. Gerar Contratos (Consumer Tests)
```bash
mvn test -Dtest=BooksCDCDefinitionTest "-Dspring.profiles.active=cdc-test"
```
Isso gera os arquivos Pact na pasta `target/pacts/`.

### 2. Executar Testes de Integração do Consumidor
```bash
mvn test -Dtest=BooksCDCConsumerIT "-Dspring.profiles.active=cdc-test"
```

### 3. Executar Testes do Produtor (Local)
```bash
mvn test -Dtest=BooksProducerCDCIT "-Dspring.profiles.active=cdc-test"
```

### 4. Executar Testes do Produtor (Pact Broker)
```bash
mvn test -Dtest=BooksProducerFromPactBrokerCDCIT "-Dspring.profiles.active=cdc-test"
```

### 5. Executar Todos os Testes CDC
```bash
mvn test -Dtest="*CDC*" "-Dspring.profiles.active=cdc-test"
```

## Análise dos Eventos

### Eventos Consumidos pelo Books Command

| Evento | Fonte | Processamento | Handler |
|--------|-------|---------------|---------|
| `book_created` | Outros serviços | Criar book localmente | `receiveBookCreatedMsg()` |
| `book_updated` | Outros serviços | Atualizar book local | `receiveBookUpdated()` |
| `author_pending_created` | Author Service | SAGA - Author pendente | `receiveAuthorPendingCreated()` |
| `genre_pending_created` | Genre Service | SAGA - Genre pendente | `receiveGenrePendingCreated()` |
| `author_creation_failed` | Author Service | SAGA - Falha author | `receiveAuthorCreationFailed()` |
| `genre_creation_failed` | Genre Service | SAGA - Falha genre | `receiveGenreCreationFailed()` |
| `author_created` | Author Service | SAGA - Author criado | `receiveAuthorCreated()` |
| `genre_created` | Genre Service | SAGA - Genre criado | `receiveGenreCreated()` |
| `book_finalized` | Book Service | SAGA - Book finalizado | `receiveBookFinalized()` |
| `lending_validation_request` | Lending Service | Validar lending | `receiveLendingValidationRequest()` |

### Eventos Produzidos pelo Books Command

| Evento | Trigger | Destino | Publisher |
|--------|---------|---------|-----------|
| `BOOK_CREATED` | Criação de book | Outros serviços | `sendBookCreated()` |
| `BOOK_UPDATED` | Atualização de book | Outros serviços | `sendBookUpdated()` |
| `BOOK_DELETED` | Deleção de book | Outros serviços | `sendBookDeleted()` |
| `book_requested` | Solicitação de book | SAGA | `sendBookRequested()` |
| `book_finalized` | Finalização de book | SAGA | `sendBookFinalizedEvent()` |
| `lending_validation_response` | Resposta de validação | Lending Service | `sendBookValidationResponse()` |

## Configuração dos Testes

### Profile CDC
Os testes usam o profile `cdc-test` que:
- Mock do `RabbitTemplate` para evitar dependências externas
- Mock do `DirectExchange` 
- Configuração específica para CDC

### Dependências Mockadas
- `BookRepository` - Repositório de books
- `AuthorRepository` - Repositório de authors  
- `GenreRepository` - Repositório de genres
- `PendingBookRequestRepository` - Repositório de requests pendentes
- `BookEventsPublisher` - Publisher de eventos
- `BookService` - Serviço de negócio

## Validação

### Consumer Tests
- ✅ Verifica se o serviço consegue processar mensagens no formato esperado
- ✅ Valida mapeamento JSON para objetos
- ✅ Testa handlers de mensagens RabbitMQ

### Producer Tests  
- ✅ Verifica se o serviço produz mensagens no formato contratado
- ✅ Valida serialização de objetos para JSON
- ✅ Testa publishers de eventos

## Benefícios

1. **Detecção Precoce de Quebras**: Mudanças em contratos são detectadas antes da produção
2. **Documentação Viva**: Contratos servem como documentação atualizada
3. **Testes Independentes**: Não dependem de serviços externos
4. **Cobertura Completa**: Todos os eventos identificados estão cobertos
5. **Integração CI/CD**: Podem ser executados em pipelines automatizados

## Estrutura de Arquivos

```
src/test/java/pt/psoft/g1/psoftg1/cdc/
├── config/
│   └── CDCTestConfiguration.java
├── consumer/
│   ├── BooksCDCDefinitionTest.java
│   └── BooksCDCConsumerIT.java
└── producer/
    ├── BookMessageBuilder.java
    ├── BooksProducerCDCIT.java
    └── BooksProducerFromPactBrokerCDCIT.java
```

## Próximos Passos

1. **Integrar com CI/CD**: Adicionar execução automática nos pipelines
2. **Pact Broker**: Configurar broker central para gestão de contratos  
3. **Monitoramento**: Implementar alertas para quebras de contrato
4. **Documentação**: Gerar documentação automática dos contratos

## Manutenção

- **Atualizar contratos** quando houver mudanças nos eventos
- **Executar regularmente** para garantir compatibilidade
- **Versionar contratos** para gestão de mudanças
- **Revisar cobertura** quando novos eventos forem adicionados

---

*Documentação gerada em: 03/01/2026*  
*Baseado no padrão estabelecido em: lms_readers_command*
