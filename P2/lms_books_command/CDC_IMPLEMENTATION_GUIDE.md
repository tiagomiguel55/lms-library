# CDC Tests Implementation - lms_books_command

## 📚 Visão Geral

Os testes CDC (Consumer-Driven Contract) no `lms_books_command` garantem que:
1. **Como CONSUMIDOR**: Books consegue processar mensagens de outros serviços
2. **Como PRODUTOR**: Books produz mensagens no formato esperado por outros serviços

**Framework usado:** Pact (biblioteca Java para CDC testing)

---

## 🏗️ Arquitetura dos Testes CDC

```
lms_books_command/src/test/java/pt/psoft/g1/psoftg1/CDCTests/
├── config/
│   └── CDCTestConfiguration.java      // Configuração de mocks para testes
├── consumer/
│   ├── BooksCDCDefinitionTest.java    // DEFINE contratos esperados
│   └── BooksCDCConsumerIT.java        // TESTA consumo de mensagens
└── producer/
    ├── BooksProducerCDCIT.java        // VALIDA produção de mensagens
    ├── BooksProducerFromPactBrokerCDCIT.java  // (DESABILITADO - precisa Pact Broker)
    └── BookMessageBuilder.java        // Helper para construir mensagens
```

---

## 🔵 PARTE 1: Books como CONSUMIDOR

### 1.1 BooksCDCDefinitionTest.java - Definição de Contratos

**O que faz:** Define os contratos das mensagens que Books **espera receber** de outros serviços.

**Exemplo prático:**

```java
@Pact(consumer = "book_created-consumer")
V4Pact createBookCreatedPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringType("isbn", "9780140449136");
    body.stringType("title", "The Odyssey");
    body.stringType("description", "An epic poem by Homer");
    body.stringType("genre", "Classic Literature");
    body.array("authorIds")
            .integerType(1)
            .closeArray();
    body.stringMatcher("version", "[0-9]+", "1");

    return builder.expectsToReceive("a book created event")
            .withMetadata(metadata)
            .withContent(body)
            .toPact();
}
```

**O que acontece:**
1. Define a estrutura esperada da mensagem `book.created`
2. Especifica tipos de dados: `stringType`, `integerType`, `array`
3. Define validações: `stringMatcher` (regex)
4. Gera ficheiro JSON em `target/pacts/book_created-consumer-book_event-producer.json`

**Contratos definidos:**
- `book_created` - Livro criado
- `book_updated` - Livro atualizado
- `book_requested` - Requisição de livro
- `book_finalized` - Livro finalizado
- `author_created` - Autor criado
- `author_pending_created` - Autor pendente
- `author_creation_failed` - Falha na criação de autor
- `genre_created` - Género criado
- `genre_pending_created` - Género pendente
- `genre_creation_failed` - Falha na criação de género
- `lending_validation_request` - Validação de empréstimo

**Output:** Gera 11 ficheiros `.json` em `target/pacts/`

---

### 1.2 BooksCDCConsumerIT.java - Teste de Consumo

**O que faz:** Testa se o `BookRabbitmqController` consegue **processar corretamente** as mensagens definidas nos contratos.

**Fluxo do teste:**

```java
@Test
void testBookCreatedMessageProcessing() throws Exception {
    // 1. Carrega o contrato do ficheiro pact
    File pactFile = new File("target/pacts/book_created-consumer-book_event-producer.json");
    Pact pact = pactReader.loadPact(pactFile);
    
    // 2. Mock dos repositórios
    when(bookRepository.findByIsbn(anyString())).thenReturn(Optional.empty());
    when(authorRepository.findByAuthorNumber(anyLong())).thenReturn(Optional.of(mock(Author.class)));
    
    // 3. Obtém a mensagem do contrato
    List<Message> messagesGeneratedByPact = pact.asMessagePact().get().getMessages();
    
    // 4. Para cada mensagem, testa o listener
    for (Message messageGeneratedByPact : messagesGeneratedByPact) {
        String jsonReceived = messageGeneratedByPact.contentsAsString();
        
        // 5. Cria mensagem Spring AMQP
        org.springframework.amqp.core.Message message = 
            new org.springframework.amqp.core.Message(jsonReceived.getBytes(), messageProperties);
        
        // 6. Testa se o listener processa sem erros
        assertDoesNotThrow(() -> {
            listener.receiveBookCreatedMsg(message);
        });
    }
}
```

**Validações:**
- ✅ Mensagem é deserializada corretamente
- ✅ Listener não lança exceções
- ✅ Lógica de negócio funciona (com mocks)

---

## 🟢 PARTE 2: Books como PRODUTOR

### 2.1 BooksProducerCDCIT.java - Validação de Produção

**O que faz:** Valida se Books **produz mensagens corretas** que outros serviços esperam consumir.

**Fluxo do teste:**

```java
@Provider("book_event-producer")
@PactFolder("target/pacts")  // ← Lê contratos locais
@SpringBootTest(classes = {CDCTestConfiguration.class})
@ActiveProfiles("cdc-test")
public class BooksProducerCDCIT {

    @PactVerifyProvider("a book created event")
    MessageAndMetadata bookCreated() throws JsonProcessingException {
        // 1. Cria um objeto BookViewAMQP com dados de teste
        BookViewAMQP bookView = new BookViewAMQP();
        bookView.setIsbn("9780140449136");
        bookView.setTitle("The Odyssey");
        bookView.setDescription("An epic poem by Homer");
        bookView.setGenre("Classic Literature");
        bookView.setAuthorIds(List.of(1L));
        bookView.setVersion(1L);

        // 2. Serializa para JSON (como seria enviado para RabbitMQ)
        byte[] payload = new ObjectMapper().writeValueAsBytes(bookView);

        // 3. Retorna mensagem com metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");
        
        return new MessageAndMetadata(payload, metadata);
    }
}
```

**O que é validado:**
1. Pact framework lê o contrato de `target/pacts/book_created-consumer-book_event-producer.json`
2. Compara a mensagem produzida com o contrato esperado
3. Valida:
   - ✅ Todos os campos obrigatórios existem
   - ✅ Tipos de dados estão corretos
   - ✅ Valores seguem as regex definidas
   - ✅ Estrutura JSON está correta

**Se falhar:** O teste quebra e mostra exatamente qual campo não está conforme o contrato.

---

## 🔧 PARTE 3: Configuração

### 3.1 CDCTestConfiguration.java

**O que faz:** Fornece mocks de RabbitMQ para os testes não precisarem de broker real.

```java
@TestConfiguration
@Profile("cdc-test")
public class CDCTestConfiguration {
    
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);  // Mock do RabbitMQ
    }

    @Bean
    @Primary
    public DirectExchange directExchange() {
        return Mockito.mock(DirectExchange.class);  // Mock da exchange
    }
}
```

**Benefício:** Testes CDC rodam **sem dependências externas** (sem RabbitMQ, sem PostgreSQL).

---

### 3.2 application-cdc-test.properties

```properties
# Disable RabbitMQ AutoConfiguration
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration

# Use H2 in-memory database
spring.datasource.url=jdbc:h2:mem:testdb

# Disable RabbitMQ health indicator
management.health.rabbit.enabled=false
```

**Garante:** Testes rodam isolados, sem serviços externos.

---

## 📊 Fluxo Completo de Execução

### Quando executas: `mvn test -Dtest=pt.psoft.g1.psoftg1.CDCTests.**`

```
┌─────────────────────────────────────────────────────────────────┐
│ STEP 1: BooksCDCDefinitionTest                                  │
│ ─────────────────────────────────────────────────────────────── │
│ • Define 11 contratos de mensagens esperadas                    │
│ • Gera ficheiros JSON em target/pacts/                          │
│   - book_created-consumer-book_event-producer.json              │
│   - author_created-consumer-book_event-producer.json            │
│   - genre_created-consumer-book_event-producer.json             │
│   - ... (total: 11 ficheiros)                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 2: BooksProducerCDCIT                                      │
│ ─────────────────────────────────────────────────────────────── │
│ • Lê os 11 ficheiros pact de target/pacts/                      │
│ • Para cada contrato:                                           │
│   1. Chama @PactVerifyProvider method                           │
│   2. Obtém mensagem produzida pelo método                       │
│   3. Compara com o contrato esperado                            │
│   4. PASS ✅ se idêntico, FAIL ❌ se diferente                  │
│                                                                  │
│ Resultado: 11 testes executados, 11 passaram ✅                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ STEP 3: BooksCDCConsumerIT                                      │
│ ─────────────────────────────────────────────────────────────── │
│ • Lê ficheiros pact de target/pacts/                            │
│ • Para cada mensagem no contrato:                               │
│   1. Extrai JSON da mensagem                                    │
│   2. Cria mensagem Spring AMQP                                  │
│   3. Chama listener.receiveBookCreatedMsg(message)              │
│   4. Verifica que não lança exceção                             │
│                                                                  │
│ Resultado: Listeners processam corretamente ✅                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ RESULTADO FINAL                                                 │
│ ─────────────────────────────────────────────────────────────── │
│ ✅ Tests run: 33                                                │
│ ✅ Failures: 0                                                  │
│ ✅ Errors: 0                                                    │
│ ⏭️  Skipped: 1 (BooksProducerFromPactBrokerCDCIT)               │
│                                                                  │
│ ✅ BUILD SUCCESS                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Benefícios desta Abordagem

### 1. **Contratos Versionados**
- Contratos são ficheiros JSON versionados no Git
- Histórico completo de mudanças
- Fácil ver quando/como um contrato mudou

### 2. **Validação Bidirecional**
```
Books como CONSUMIDOR ←→ Outros serviços como PRODUTORES
Books como PRODUTOR   ←→ Outros serviços como CONSUMIDORES
```

### 3. **Sem Dependências Externas**
- ❌ Não precisa RabbitMQ
- ❌ Não precisa PostgreSQL
- ❌ Não precisa Pact Broker (para desenvolvimento local)
- ✅ Testes rodam em milissegundos

### 4. **Detecção Precoce de Quebras**
Se Books mudar o formato de uma mensagem:
```java
// ANTES
{"isbn": "123", "title": "Book"}

// DEPOIS (mudança perigosa!)
{"bookId": "123", "bookName": "Book"}
```

**O que acontece:**
1. Developer faz commit
2. Pipeline executa CDC tests
3. `BooksProducerCDCIT` **FALHA** ❌
4. Erro claro: "Expected field 'isbn', got 'bookId'"
5. Deploy é **bloqueado**
6. Developer corrige antes de quebrar outros serviços

---

## 🔍 Exemplo Prático de Falha

### Cenário: Books muda campo `isbn` para `bookId`

**Contrato esperado (target/pacts/):**
```json
{
  "isbn": "9780140449136",
  "title": "The Odyssey"
}
```

**Mensagem produzida (BooksProducerCDCIT):**
```json
{
  "bookId": "9780140449136",  // ← DIFERENTE!
  "title": "The Odyssey"
}
```

**Resultado do teste:**
```
❌ FAILED: a book created event
   Expected field 'isbn' but got 'bookId'
   
   Contract mismatch:
   - Missing field: isbn
   - Unexpected field: bookId
```

**Pipeline:** ❌ BLOQUEIA deploy

---

## 📁 Ficheiros Gerados

Quando executas os testes, são gerados:

```
target/pacts/
├── author_created-consumer-book_event-producer.json
├── author_creation_failed-consumer-book_event-producer.json
├── author_pending_created-consumer-book_event-producer.json
├── book_created-consumer-book_event-producer.json
├── book_finalized-consumer-book_event-producer.json
├── book_requested-consumer-book_event-producer.json
├── book_updated-consumer-book_event-producer.json
├── genre_created-consumer-book_event-producer.json
├── genre_creation_failed-consumer-book_event-producer.json
├── genre_pending_created-consumer-book_event-producer.json
└── lending_validation_request-consumer-book_event-producer.json
```

**Estrutura de um ficheiro pact:**
```json
{
  "consumer": {
    "name": "book_created-consumer"
  },
  "provider": {
    "name": "book_event-producer"
  },
  "messages": [
    {
      "description": "a book created event",
      "contents": {
        "isbn": "9780140449136",
        "title": "The Odyssey",
        "genre": "Classic Literature"
      },
      "matchingRules": {
        "body": {
          "$.isbn": {
            "matchers": [{"match": "type"}]
          }
        }
      }
    }
  ]
}
```

---

## 🚀 Como Executar

### Localmente (PowerShell):
```powershell
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_command
mvn test "-Dspring.profiles.active=test" "-Dtest=pt.psoft.g1.psoftg1.CDCTests.**"
```

### Na Pipeline (Jenkins):
```bash
docker build -f DockerfileTests -t lmsbooks-tests:cdc .
docker run --rm -e TEST_TYPE=CDCTests lmsbooks-tests:cdc
```

**Tempo de execução:** ~50 segundos para 33 testes

---

## 🔄 Ciclo de Vida dos Contratos

```
1. DESENVOLVIMENTO
   └─> Developer muda código
       └─> Executa CDC tests localmente
           └─> Testes PASSAM ✅

2. COMMIT & PUSH
   └─> CI/CD pipeline
       └─> Executa CDC tests
           └─> Valida contratos

3. DEPLOY
   └─> Se CDC tests PASSAM ✅
       └─> Deploy permitido
   └─> Se CDC tests FALHAM ❌
       └─> Deploy BLOQUEADO

4. RUNTIME
   └─> Mensagens reais seguem os contratos
       └─> Sem surpresas em produção
```

---

## 🎓 Conceitos Importantes

### Consumer-Driven Contract (CDC)
- **Consumer** define o que espera receber
- **Provider** deve cumprir o contrato
- Inverte a responsabilidade: consumidor tem poder

### Pact Framework
- Framework open-source para CDC testing
- Suporta múltiplas linguagens (Java, JS, Go, etc.)
- Integração com JUnit 5

### Message Pact vs HTTP Pact
- **HTTP Pact**: REST APIs (request/response)
- **Message Pact**: Mensageria assíncrona (events/commands)
- Books usa **Message Pact** (RabbitMQ)

---

## 📚 Conclusão

Os testes CDC no `lms_books_command`:

✅ **Validam contratos de mensagens** em ambas as direções  
✅ **Rodam sem dependências externas** (RabbitMQ, PostgreSQL)  
✅ **Detectam quebras antes de produção**  
✅ **Executam rapidamente** (~50 segundos)  
✅ **Integrados na pipeline** (staging only)  
✅ **Contratos versionados no Git**  

**Resultado:** Confiança que as mensagens entre serviços estão sempre compatíveis! 🎉

