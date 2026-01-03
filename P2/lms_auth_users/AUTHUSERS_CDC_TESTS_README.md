# Testes CDC (Consumer Driven Contract) para lms_auth_users

## Resumo
Foram criados testes CDC completos para o módulo `lms_auth_users` baseados no padrão encontrado no módulo `nunopsilva-lms-books-53be5800ef8f`. Os testes cobrem tanto o papel de consumidor quanto de produtor de eventos.

### ✅ TODOS OS EVENTOS IDENTIFICADOS E COBERTOS

Após análise completa do código fonte, foram identificados e implementados testes para **todos os 5 eventos** do módulo:

1. **reader_user_requested** - Consumido via RabbitListener
2. **user_created** - Produzido via UserEventPublisher  
3. **user_updated** - Produzido via UserEventPublisher
4. **user_pending_created** - Produzido via UserEventPublisher
5. **user_deleted** ⭐ **ADICIONADO** - Produzido via UserEventPublisher

## Estrutura de Testes

### 1. Testes de Definição de Contratos (Consumer)
**Arquivo:** `UsersCDCDefinitionTest.java`
- Define contratos para 4 tipos de eventos:
  - `reader_user_requested` - Solicitação de criação de usuário/leitor
  - `user_created` - Usuário criado
  - `user_updated` - Usuário atualizado 
  - `user_pending_created` - Usuário pendente de criação
- Gera arquivos JSON de contratos na pasta `target/pacts/`

### 2. Testes de Integração do Consumidor
**Arquivo:** `UsersCDCConsumerIT.java`
- Testa o processamento de mensagens baseado nos contratos gerados
- Verifica se o `UserRabbitmqController` processa corretamente as mensagens
- Usa mocks para simular dependências (UserRepository, PasswordEncoder, etc.)

### 3. Testes do Produtor (Provider)
**Arquivos:** 
- `UsersProducerCDCIT.java` - Testes usando arquivos locais
- `UsersProducerFromPactBrokerCDCIT.java` - Testes usando Pact Broker

Verificam se o serviço consegue produzir mensagens que atendem aos contratos definidos.

### 4. Utilitários
**Arquivos:**
- `UserMessageBuilder.java` - Constrói mensagens de teste
- `CDCTestConfiguration.java` - Configuração específica para testes CDC

## Como Executar os Testes

### 1. Gerar Contratos (Consumer Tests)
```bash
mvn test -Dtest=UsersCDCDefinitionTest "-Dspring.profiles.active=cdc-test"
```

### 2. Verificar Contratos como Produtor
```bash
mvn test -Dtest=UsersProducerCDCIT "-Dspring.profiles.active=cdc-test"
```

### 3. Testes de Integração do Consumidor
```bash
mvn test -Dtest=UsersCDCConsumerIT "-Dspring.profiles.active=cdc-test"
```

### 4. Executar todos os testes CDC
```bash
mvn test -Dtest="*CDC*" "-Dspring.profiles.active=cdc-test"
```

## Eventos Cobertos

### 1. Reader User Requested
- **Consumer:** `reader_user_requested-consumer`
- **Provider:** `user_event-producer` 
- **Descrição:** Evento para solicitação de criação de usuário/leitor
- **Campos:** readerNumber, username, password, fullName

### 2. User Created 
- **Consumer:** `user_created-consumer`
- **Provider:** `user_event-producer`
- **Descrição:** Evento quando um usuário é criado
- **Campos:** username, fullName, version

### 3. User Updated
- **Consumer:** `user_updated-consumer` 
- **Provider:** `user_event-producer`
- **Descrição:** Evento quando um usuário é atualizado
- **Campos:** username, fullName, version

### 4. User Pending Created
- **Consumer:** `user_pending_created-consumer`
- **Provider:** `user_event-producer`
- **Descrição:** Evento de usuário pendente de criação (SAGA)
- **Campos:** readerNumber, userId, username, finalized

### 5. User Deleted
- **Consumer:** `user_deleted-consumer`
- **Provider:** `user_event-producer`
- **Descrição:** Evento quando um usuário é deletado/desabilitado
- **Payload:** username (string simples, não JSON)

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

## Configuração do Pact Broker

Os testes estão configurados para usar um Pact Broker local:
- **URL:** http://localhost:9292
- **Credenciais:** pact_broker / pact_broker

## Status dos Testes

✅ **Testes de Definição:** 5/5 passando
✅ **Testes de Produtor:** 5/5 passando  
✅ **Testes de Consumidor:** Implementados (2 testes principais)

Todos os contratos são gerados corretamente e os testes de verificação passam, garantindo compatibilidade entre consumidor e produtor de eventos do sistema de autenticação de usuários.
