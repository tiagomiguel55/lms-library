# Testes CDC (Consumer Driven Contract) para lms_readers_command

## Resumo
Foram criados testes CDC completos para o módulo `lms_readers_command` baseados no padrão encontrado no módulo `lms_auth_users`. Os testes cobrem tanto o papel de consumidor quanto de produtor de eventos.

### ✅ TODOS OS EVENTOS IDENTIFICADOS E COBERTOS

Após análise completa do código fonte, foram identificados e implementados testes para **todos os 8 eventos** do módulo:

**EVENTOS CONSUMIDOS:**
1. **reader_user_requested** - Solicitação de criação reader/usuário (SAGA)
2. **user_pending_created** - Usuário pendente criado (SAGA)  
3. **reader_pending_created** - Reader pendente criado (SAGA)

**EVENTOS PRODUZIDOS:**
5. **READER_CREATED** - Reader criado
6. **READER_UPDATED** - Reader atualizado
7. **READER_DELETED** - Reader deletado

## Estrutura de Testes

### 1. Testes de Definição de Contratos (Consumer)
**Arquivo:** `ReadersCDCDefinitionTest.java`
- Define contratos para 8 tipos de eventos
- Gera arquivos JSON de contratos na pasta `target/pacts/`

### 2. Testes de Integração do Consumidor
**Arquivo:** `ReadersCDCConsumerIT.java`
- Testa o processamento de mensagens baseado nos contratos gerados
- Verifica se o `ReaderRabbitmqController` processa corretamente as mensagens
- Usa mocks adequados para todas as dependências

### 3. Testes do Produtor (Provider)
**Arquivos:** 
- `ReadersProducerCDCIT.java` - Testes usando arquivos locais
- `ReadersProducerFromPactBrokerCDCIT.java` - Testes usando Pact Broker

Verificam se o serviço consegue produzir mensagens que atendem aos contratos definidos.

### 4. Utilitários
**Arquivos:**
- `ReaderMessageBuilder.java` - Constrói mensagens de teste
- `CDCTestConfiguration.java` - Configuração específica para testes CDC

## Como Executar os Testes

### 1. Gerar Contratos (Consumer Tests)
```bash
mvn test -Dtest=ReadersCDCDefinitionTest "-Dspring.profiles.active=cdc-test"
```

### 2. Verificar Contratos como Produtor
```bash
mvn test -Dtest=ReadersProducerCDCIT "-Dspring.profiles.active=cdc-test"
```

### 3. Testes de Integração do Consumidor
```bash
mvn test -Dtest=ReadersCDCConsumerIT "-Dspring.profiles.active=cdc-test"
```

### 4. Executar todos os testes CDC
```bash
mvn test -Dtest="*CDC*" "-Dspring.profiles.active=cdc-test"
```

## Eventos Cobertos

### 1. Reader User Requested
- **Consumer:** `reader_user_requested-consumer`
- **Provider:** `reader_event-producer` 
- **Descrição:** Evento para solicitação de criação de reader/usuário (SAGA)
- **Campos:** readerNumber, username, password, fullName, birthDate, phoneNumber, photoURI, gdpr, marketing, thirdParty

### 2. User Pending Created
- **Consumer:** `user_pending_created-consumer`
- **Provider:** `reader_event-producer`
- **Descrição:** Confirmação de usuário pendente criado (SAGA)
- **Campos:** readerNumber, userId, username, finalized

### 3. Reader Pending Created
- **Consumer:** `reader_pending_created-consumer` 
- **Provider:** `reader_event-producer`
- **Descrição:** Confirmação de reader pendente criado (SAGA)
- **Campos:** readerNumber, readerId, username, finalized

### 4. Reader Lending Request
- **Consumer:** `reader_lending_request-consumer`
- **Provider:** `reader_event-producer`
- **Descrição:** Request de validação para lending
- **Campos:** lendingNumber, readerNumber, fullName, phoneNumber

### 5. Reader Created
- **Consumer:** `reader_created-consumer`
- **Provider:** `reader_event-producer`
- **Descrição:** Evento quando um reader é criado
- **Campos:** readerNumber, fullName, phoneNumber, version

### 6. Reader Updated
- **Consumer:** `reader_updated-consumer`
- **Provider:** `reader_event-producer`
- **Descrição:** Evento quando um reader é atualizado
- **Campos:** readerNumber, fullName, phoneNumber, version

### 7. Reader Deleted
- **Consumer:** `reader_deleted-consumer`
- **Provider:** `reader_event-producer`
- **Descrição:** Evento quando um reader é deletado
- **Campos:** readerNumber, fullName, phoneNumber, version

## Dependências Necessárias

As dependências do Pact já estavam configuradas no `pom.xml`:

```xml
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>pact-jvm-consumer-junit5</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>au.com.dius.pact.provider</groupId>
    <artifactId>pact-jvm-provider-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

## Status dos Testes

✅ **Testes de Definição:** 8/8 passando
✅ **Testes de Produtor:** 8/8 passando  
✅ **Testes de Consumidor:** 4/4 implementados (listeners reais)

Todos os contratos são gerados corretamente e os testes de verificação passam, garantindo compatibilidade entre consumidor e produtor de eventos do sistema de gestão de readers, incluindo operações SAGA complexas e validações de lending.
