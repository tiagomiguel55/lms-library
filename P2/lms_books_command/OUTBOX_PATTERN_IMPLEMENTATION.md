# Outbox Pattern Implementation

## 📋 Overview

O **Outbox Pattern** foi implementado no `lms_books_command` para garantir **consistência eventual** e **entrega confiável** de eventos entre o microserviço e o RabbitMQ.

Este pattern é especialmente crítico no fluxo do **endpoint `/create-complete`**, onde múltiplos bounded contexts (Book, Author, Genre) colaboram numa **SAGA** para criar um livro completo de forma assíncrona.

## 🎯 Problema Resolvido

**Antes (sem Outbox):**
- Serviço salva dados no BD ✅
- Serviço tenta publicar evento no RabbitMQ ❌ (falha se RabbitMQ estiver down)
- **Resultado:** Dados guardados mas evento perdido → Inconsistência!
- **No `/create-complete`:** A SAGA ficaria presa indefinidamente esperando por eventos que nunca chegam

**Depois (com Outbox):**
- Serviço salva dados no BD ✅
- Serviço salva evento na tabela `outbox_events` ✅ (mesma transação)
- Background job publica evento no RabbitMQ ✅ (com retry automático)
- **Resultado:** Garantia de entrega eventual do evento!
- **No `/create-complete`:** A SAGA completa com sucesso mesmo que o RabbitMQ esteja temporariamente indisponível

## 🏗️ Arquitetura Detalhada

### Visão Geral: Um único processo com 3 bounded contexts

```
┌────────────────────────────────────────────────────────────────────────┐
│                    lms_books_command (1 processo Spring Boot)          │
│                                                                         │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │  BookManagement  │  │ AuthorManagement │  │ GenreManagement  │   │
│  │   (Bounded       │  │   (Bounded       │  │   (Bounded       │   │
│  │    Context)      │  │    Context)      │  │    Context)      │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │              Shared Infrastructure                            │    │
│  │  - OutboxEvent (tabela)                                      │    │
│  │  - OutboxService                                             │    │
│  │  - OutboxPublisher (background job)                          │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │              PostgreSQL Database                              │    │
│  │  - books, authors, genres (tabelas de negócio)              │    │
│  │  - outbox_events (tabela do pattern)                         │    │
│  │  - pending_book_request (SAGA state)                         │    │
│  └──────────────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
                          ┌─────────────────┐
                          │   RabbitMQ      │
                          │   (Exchanges)   │
                          └─────────────────┘
```

### Fluxo Detalhado: POST /api/books/create-complete

```
1. HTTP Request chega
   ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 1: BookController recebe request                          │
│ POST /api/books/create-complete                                 │
│ Body: {"isbn":"123", "authorName":"Orwell", "genreName":"Sci-Fi"}│
└─────────────────────────────────────────────────────────────────┘
   ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 2: BookService.createWithAuthorAndGenre()                 │
│ @Transactional ← IMPORTANTE!                                    │
│                                                                  │
│   [BD] INSERT INTO pending_book_request (...)                   │
│        status = PENDING_AUTHOR_CREATION                         │
│                                                                  │
│   [Outbox] bookEventsPublisher.sendBookRequestedEvent(...)     │
│            ↓ chama OutboxService.saveEvent(...)                 │
│            ↓ INSERT INTO outbox_events                          │
│              (aggregate_type='Book',                            │
│               event_type='BOOK_REQUESTED',                      │
│               payload='{"bookId":"123",...}',                   │
│               processed=false)                                  │
│                                                                  │
│   COMMIT da transação → Ambos salvos atomicamente! ✅          │
│                                                                  │
│   Return null → Controller retorna HTTP 202 Accepted            │
└─────────────────────────────────────────────────────────────────┘
   ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASSO 3: OutboxPublisher (roda a cada 5 segundos)               │
│ @Scheduled(fixedDelay=5000)                                     │
│                                                                  │
│   1. Query: SELECT * FROM outbox_events WHERE processed=false   │
│                                                                  │
│   2. Para cada evento:                                          │
│      determineExchange("Book") → "books.exchange"              │
│      rabbitTemplate.convertAndSend(                             │
│          "books.exchange",        ← Nome do exchange            │
│          "BOOK_REQUESTED",        ← Routing key                 │
│          payload                  ← JSON do evento              │
│      )                                                          │
│                                                                  │
│   3. UPDATE outbox_events SET processed=true, processed_at=NOW()│
└─────────────────────────────────────────────────────────────────┘
   ↓
   ↓ Evento publicado no RabbitMQ
   ↓
┌────────────────────────────┬────────────────────────────────────┐
│                            │                                    │
▼                            ▼                                    │
┌──────────────────┐   ┌──────────────────┐                     │
│ PASSO 4a:        │   │ PASSO 4b:        │                     │
│ AuthorRabbitmq   │   │ GenreRabbitmq    │                     │
│ Controller       │   │ Controller       │                     │
│ (mesmo processo!)│   │ (mesmo processo!)│                     │
└──────────────────┘   └──────────────────┘                     │
│                      │                                         │
│ @RabbitListener      │ @RabbitListener                        │
│ Recebe BOOK_REQUESTED│ Recebe BOOK_REQUESTED                 │
│                      │                                         │
│ 1. Cria Author       │ 1. Cria Genre                          │
│    (finalized=false) │    (finalized=false)                   │
│                      │                                         │
│ 2. @Transactional    │ 2. @Transactional                      │
│    INSERT author     │    INSERT genre                        │
│    INSERT outbox     │    INSERT outbox                       │
│    (AUTHOR_PENDING_  │    (GENRE_PENDING_                     │
│     CREATED)         │     CREATED)                           │
│                      │                                         │
│ 3. Return            │ 3. Return                              │
└──────────┬───────────┴──────────┬─────────────────────────────┘
           │                      │
           │ (5 segundos depois)  │
           ↓                      ↓
    OutboxPublisher publica ambos eventos
           │                      │
           └──────────┬───────────┘
                      ↓
         ┌────────────────────────────┐
         │ PASSO 5: BookRabbitmq      │
         │ Controller recebe AMBOS    │
         │ eventos                    │
         │                            │
         │ Quando tem os 2:           │
         │ - AUTHOR_PENDING_CREATED   │
         │ - GENRE_PENDING_CREATED    │
         │                            │
         │ @Transactional             │
         │ UPDATE pending_book_request│
         │ SET status=                │
         │   BOTH_PENDING_CREATED     │
         │                            │
         │ INSERT INTO outbox_events  │
         │ (BOOK_FINALIZED)           │
         └────────────────────────────┘
                      ↓
         OutboxPublisher publica BOOK_FINALIZED
                      ↓
         ┌────────────┴───────────┐
         ↓                        ↓
┌──────────────────┐    ┌──────────────────┐
│ PASSO 6a:        │    │ PASSO 6b:        │
│ AuthorRabbitmq   │    │ GenreRabbitmq    │
│ recebe           │    │ recebe           │
│ BOOK_FINALIZED   │    │ BOOK_FINALIZED   │
│                  │    │                  │
│ @Transactional   │    │ @Transactional   │
│ UPDATE author    │    │ UPDATE genre     │
│ SET finalized=   │    │ SET finalized=   │
│   true           │    │   true           │
│                  │    │                  │
│ INSERT outbox    │    │ INSERT outbox    │
│ (AUTHOR_CREATED) │    │ (GENRE_CREATED)  │
│ com bookId! ✅   │    │ com bookId! ✅   │
└──────────────────┘    └──────────────────┘
         │                       │
         │ (5 segundos depois)   │
         ↓                       ↓
    OutboxPublisher publica ambos
         │                       │
         └───────────┬───────────┘
                     ↓
         ┌────────────────────────────┐
         │ PASSO 7: BookRabbitmq      │
         │ recebe AMBOS eventos:      │
         │ - AUTHOR_CREATED (bookId!) │
         │ - GENRE_CREATED (bookId!)  │
         │                            │
         │ @Transactional             │
         │ INSERT INTO books (...)    │
         │                            │
         │ UPDATE pending_book_request│
         │ SET status=BOOK_CREATED    │
         │                            │
         │ 🎉 SAGA COMPLETA!          │
         └────────────────────────────┘
```

### Explicação dos Componentes

#### 1. **Bounded Contexts (dentro do mesmo processo)**
```
lms_books_command/
├── bookmanagement/          ← BookManagement BC
│   ├── model/Book
│   ├── repositories/
│   ├── services/BookService
│   └── publishers/BookEventsPublisher
│
├── authormanagement/        ← AuthorManagement BC
│   ├── model/Author
│   ├── repositories/
│   ├── services/AuthorService
│   └── publishers/AuthorEventsPublisher
│
├── genremanagement/         ← GenreManagement BC
│   ├── model/Genre
│   ├── repositories/
│   ├── services/GenreService
│   └── publishers/GenreEventsPublisher
│
└── shared/                  ← Shared Infrastructure
    ├── model/OutboxEvent
    ├── repositories/OutboxEventRepository
    ├── services/OutboxService
    └── services/OutboxPublisher  ← Background Job!
```

#### 2. **Database Tables**

```sql
-- Tabelas de Negócio
books (isbn, title, description, ...)
authors (author_number, name, bio, finalized, ...)
genres (pk, genre, finalized, ...)

-- Tabela do Outbox Pattern
outbox_events (
    id BIGINT PRIMARY KEY,
    aggregate_type VARCHAR,    -- "Book", "Author", "Genre"
    aggregate_id VARCHAR,      -- ISBN, AuthorID, GenreName
    event_type VARCHAR,        -- "BOOK_REQUESTED", etc
    payload TEXT,              -- JSON do evento
    processed BOOLEAN,         -- false até ser publicado
    processed_at TIMESTAMP,
    retry_count INT,
    created_at TIMESTAMP
)

-- Tabela de Estado da SAGA
pending_book_request (
    id BIGINT PRIMARY KEY,
    book_id VARCHAR,
    status VARCHAR,            -- PENDING_AUTHOR_CREATION, etc
    author_pending_received BOOLEAN,
    genre_pending_received BOOLEAN,
    author_finalized_received BOOLEAN,
    genre_finalized_received BOOLEAN,
    ...
)
```

#### 3. **RabbitMQ Exchanges e Queues**

```
Exchanges:
├── books.exchange (DirectExchange)
│   ├── Routing Key: BOOK_REQUESTED
│   │   ├─→ autoDeleteQueue_Book_Requested_Author  ← AuthorCmd ouve aqui
│   │   └─→ autoDeleteQueue_Book_Requested_Genre   ← GenreCmd ouve aqui
│   │
│   └── Routing Key: BOOK_FINALIZED
│       ├─→ autoDeleteQueue_Book_Finalized_Author  ← AuthorCmd ouve aqui
│       └─→ autoDeleteQueue_Book_Finalized_Genre   ← GenreCmd ouve aqui
│
├── authors.exchange (DirectExchange)
│   ├── Routing Key: AUTHOR_PENDING_CREATED
│   │   └─→ autoDeleteQueue_Author_Pending_Created ← BookCmd ouve aqui
│   │
│   └── Routing Key: AUTHOR_CREATED
│       └─→ autoDeleteQueue_Author_Created         ← BookCmd ouve aqui
│
└── genres.exchange (DirectExchange)
    ├── Routing Key: GENRE_PENDING_CREATED
    │   └─→ autoDeleteQueue_Genre_Pending_Created  ← BookCmd ouve aqui
    │
    └── Routing Key: GENRE_CREATED
        └─→ autoDeleteQueue_Genre_Created          ← BookCmd ouve aqui
```

#### 4. **OutboxPublisher - O "Motor" do Pattern**

```java
@Component
@Slf4j
public class OutboxPublisher {
    
    // Roda a cada 5 segundos
    @Scheduled(fixedDelayString = "5000")
    public void publishPendingEvents() {
        
        // 1. Busca eventos pendentes
        List<OutboxEvent> events = outboxService.getUnprocessedEvents();
        //    SELECT * FROM outbox_events WHERE processed=false
        
        for (OutboxEvent event : events) {
            
            // 2. Determina o exchange correto
            String exchange = determineExchange(event.getAggregateType());
            //    "Book" → "books.exchange"
            //    "Author" → "authors.exchange"
            //    "Genre" → "genres.exchange"
            
            // 3. Publica no RabbitMQ
            rabbitTemplate.convertAndSend(
                exchange,               // Nome do exchange
                event.getEventType(),   // Routing key (ex: "BOOK_REQUESTED")
                event.getPayload()      // JSON payload
            );
            
            // 4. Marca como processado
            outboxService.markAsProcessed(event.getId());
            //    UPDATE outbox_events SET processed=true WHERE id=...
        }
    }
}
```

## 📦 Componentes Implementados

### 1. **OutboxEvent** (Entity)
Tabela que armazena eventos pendentes:
- `id` - Primary key
- `aggregateType` - "Book", "Author", "Genre"
- `aggregateId` - ISBN, AuthorId, etc.
- `eventType` - "BOOK_REQUESTED", "AUTHOR_PENDING_CREATED", etc.
- `payload` - JSON do evento
- `processed` - boolean (se foi publicado)
- `retryCount` - número de tentativas
- `errorMessage` - última mensagem de erro

**Localização:** `shared/model/OutboxEvent.java`

### 2. **OutboxEventRepository**
Repository JPA para aceder à tabela Outbox.

**Localização:** `shared/repositories/OutboxEventRepository.java`

### 3. **OutboxService**
Serviço que gere eventos na Outbox:
- `saveEvent()` - Salva evento na tabela (chamado dentro de @Transactional)
- `getUnprocessedEvents()` - Obtém eventos pendentes
- `markAsProcessed()` - Marca evento como publicado
- `recordFailure()` - Regista falha de publicação

**Localização:** `shared/services/OutboxService.java`

### 4. **OutboxPublisher**
Background job (Scheduled) que processa eventos:
- **Job 1:** Runs every 5 seconds - processa eventos pendentes
- **Job 2:** Runs every 1 minuto - retry de eventos falhados
- Máximo 5 retries por evento
- Publica no exchange correto baseado no `aggregateType`

**Localização:** `shared/services/OutboxPublisher.java`

### 5. **Publishers Refatorados**
Todos os publishers foram atualizados para usar Outbox:
- `BookEventsRabbitmqPublisherImpl` ✅
- `AuthorEventsRabbitmqPublisherImpl` ✅
- `GenreEventsRabbitmqPublisherImpl` ✅

Em vez de publicar diretamente no RabbitMQ, agora salvam na Outbox.

### 6. **Services com @Transactional**
Todos os serviços que publicam eventos têm `@Transactional`:
- `BookServiceImpl` ✅
- `AuthorServiceImpl` ✅
- `GenreServiceImpl` ✅

Isto garante que salvar dados + salvar evento na outbox é **atómico**.

## ⚙️ Configuração

No `application.properties`:

```properties
##
## Outbox Pattern Configuration
##
# How often to check for pending events (in milliseconds) - every 5 seconds
outbox.polling-interval=5000
# How often to retry failed events (in milliseconds) - every minute
outbox.retry-interval=60000
# Maximum number of retries before giving up
outbox.max-retries=5
```

## 🔄 Fluxo de Execução - Endpoint `/create-complete`

### Exemplo Real: Criar um livro completo com Author e Genre

**Request:**
```bash
POST /api/books/create-complete
Content-Type: application/json

{
  "isbn": "9780451524935",
  "title": "1984",
  "description": "Dystopian novel",
  "authorName": "George Orwell",
  "genreName": "Dystopian"
}
```

### Passo 1: BookService recebe o pedido (HTTP 202 Accepted)

```java
@Transactional
public Book createWithAuthorAndGenre(BookRequestedEvent request) {
    // 1. Save pending request to database
    PendingBookRequest pendingRequest = new PendingBookRequest(isbn, authorName, genreName);
    pendingBookRequestRepository.save(pendingRequest);
    
    // 2. Save BOOK_REQUESTED event to outbox table (SAME TRANSACTION!)
    bookEventsPublisher.sendBookRequestedEvent(isbn, authorName, genreName);
    //    ↓ internamente chama:
    //    outboxService.saveEvent("Book", isbn, "BOOK_REQUESTED", eventPayload)
    
    // 3. COMMIT - both pending request and event are saved atomically
    return null; // Returns null to indicate async processing (HTTP 202)
}
```

**Logs:**
```
✅ OUTBOX: Event saved - ID: 1 | Type: BOOK_REQUESTED | Aggregate: Book:9780451524935
   Payload: {"bookId":"9780451524935","authorName":"George Orwell","genreName":"Dystopian"}
```

**Tabela `outbox_events`:**
```
id | aggregate_type | event_type      | payload                           | processed
1  | Book           | BOOK_REQUESTED  | {"bookId":"9780451524935",...}   | false
```

### Passo 2: OutboxPublisher processa o evento (5 segundos depois)

```java
@Scheduled(fixedDelayString = "5000")
public void publishPendingEvents() {
    List<OutboxEvent> events = outboxService.getUnprocessedEvents();
    
    for (OutboxEvent event : events) {
        // Determine correct exchange based on aggregate type
        String exchangeName = determineExchange(event.getAggregateType()); // "books.exchange"
        
        // Publish to RabbitMQ with routing key
        rabbitTemplate.convertAndSend(
            exchangeName,           // "books.exchange"
            event.getEventType(),   // "BOOK_REQUESTED"
            event.getPayload()      // JSON string
        );
        
        // Mark as processed
        outboxService.markAsProcessed(event.getId());
    }
}
```

**Logs:**
```
🔍 OutboxService: Found 1 unprocessed events
📤 Publishing event 1 - Type: BOOK_REQUESTED - Exchange: books.exchange
✅ Successfully published event 1
```

**Tabela `outbox_events` atualizada:**
```
id | event_type      | processed | processed_at
1  | BOOK_REQUESTED  | true      | 2025-12-29 18:25:14.706880
```

### Passo 3: AuthorCmd e GenreCmd recebem o evento

**AuthorRabbitmqController:**
```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Requested_Author.name}")
public void receiveBookRequested(Message msg) {
    // 1. Parse event
    BookRequestedEvent event = objectMapper.readValue(msg.getBody(), BookRequestedEvent.class);
    
    // 2. Create temporary author (finalized=false)
    Author author = new Author(authorName, "Bio for " + authorName, null);
    author = authorRepository.save(author);
    
    // 3. Publish AUTHOR_PENDING_CREATED to outbox
    authorEventsPublisher.sendAuthorPendingCreated(author.getAuthorNumber(), bookId, authorName, genreName);
}
```

**GenreRabbitmqController:**
```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Requested_Genre.name}")
public void receiveBookRequested(Message msg) {
    // 1. Parse event
    BookRequestedEvent event = objectMapper.readValue(msg.getBody(), BookRequestedEvent.class);
    
    // 2. Create temporary genre (finalized=false)
    Genre genre = new Genre(genreName);
    genre = genreRepository.save(genre);
    
    // 3. Publish GENRE_PENDING_CREATED to outbox
    genreEventsPublisher.sendGenrePendingCreated(genreName, bookId);
}
```

**Logs:**
```
[AuthorCmd] Received Book Requested - Author: George Orwell
[AuthorCmd] Creating temporary author: George Orwell (finalized=false)
✅ OUTBOX: Event saved - ID: 3 | Type: AUTHOR_PENDING_CREATED

[GenreCmd] Received Book Requested - Genre: Dystopian
[GenreCmd] Creating temporary genre: Dystopian (finalized=false)
✅ OUTBOX: Event saved - ID: 2 | Type: GENRE_PENDING_CREATED
```

### Passo 4: BookCmd recebe confirmação de ambos

Quando ambos os eventos `AUTHOR_PENDING_CREATED` e `GENRE_PENDING_CREATED` chegam:

```java
@RabbitListener(queues = "#{autoDeleteQueue_Author_Pending_Created.name}")
public void receiveAuthorPendingCreated(Message msg) {
    // Update pending request status
    pendingRequest.setStatus(BOTH_PENDING_CREATED);
    
    // Publish BOOK_FINALIZED to trigger finalization
    bookEventsPublisher.sendBookFinalizedEvent(authorId, authorName, bookId, genreName);
}
```

**Logs:**
```
📝 Both Author and Genre pending received → BOTH_PENDING_CREATED
📤 Sending BOOK_FINALIZED event...
✅ OUTBOX: Event saved - ID: 4 | Type: BOOK_FINALIZED
```

### Passo 5: AuthorCmd e GenreCmd finalizam as entidades

```java
@RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized_Author.name}")
public void receiveBookFinalized(Message msg) {
    // Mark author as finalized=true
    authorService.markAuthorAsFinalized(authorId);
    
    // Publish AUTHOR_CREATED with bookId
    authorEventsPublisher.sendAuthorCreated(author, bookId);
}
```

**Logs:**
```
[AuthorCmd] Finalizing author: George Orwell
✅ OUTBOX: Event saved - ID: 6 | Type: AUTHOR_CREATED
   Payload: {"authorNumber":202,"name":"George Orwell","bookId":"9780451524935",...}

[GenreCmd] Finalizing genre: Dystopian
✅ OUTBOX: Event saved - ID: 5 | Type: GENRE_CREATED
   Payload: {"genre":"Dystopian","bookId":"9780451524935",...}
```

### Passo 6: BookCmd cria o livro final

```java
@RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
public void receiveAuthorCreated(Message msg) {
    // Both author and genre are finalized
    if (pendingRequest.allFinalized()) {
        // Create the actual book!
        Book book = new Book(isbn, title, description, genre, List.of(author), null);
        bookRepository.save(book);
        
        pendingRequest.setStatus(BOOK_CREATED);
        
        // Next HTTP request will get 201 Created!
    }
}
```

**Logs:**
```
🎉🎉🎉 SAGA COMPLETE! Creating book: 9780451524935
✅ Book created successfully
📝 Updated pending request status to BOOK_CREATED
```

### Passo 7: Cliente faz polling e recebe HTTP 201

```bash
# Cliente faz novo request (polling)
GET /api/books/9780451524935

# Agora retorna 201 Created com o livro completo!
{
  "isbn": "9780451524935",
  "title": "1984",
  "description": "Dystopian novel",
  "authors": [{"authorNumber": 202, "name": "George Orwell"}],
  "genre": "Dystopian"
}
```

## 📊 Timeline Completa do `/create-complete`

| Tempo | Ação | Outbox Event | Status HTTP |
|-------|------|--------------|-------------|
| T+0s  | POST /create-complete | ID:1 BOOK_REQUESTED saved | **202 Accepted** |
| T+5s  | OutboxPublisher publica evento | ID:1 marked processed | - |
| T+5s  | AuthorCmd cria author temp | ID:3 AUTHOR_PENDING_CREATED saved | - |
| T+5s  | GenreCmd cria genre temp | ID:2 GENRE_PENDING_CREATED saved | - |
| T+10s | OutboxPublisher publica ID:2, ID:3 | Both marked processed | - |
| T+10s | BookCmd recebe ambos | ID:4 BOOK_FINALIZED saved | - |
| T+15s | OutboxPublisher publica ID:4 | ID:4 marked processed | - |
| T+15s | AuthorCmd finaliza author | ID:6 AUTHOR_CREATED saved | - |
| T+15s | GenreCmd finaliza genre | ID:5 GENRE_CREATED saved | - |
| T+20s | OutboxPublisher publica ID:5, ID:6 | Both marked processed | - |
| T+20s | BookCmd cria book final | - | - |
| T+25s | GET /books/{isbn} | - | **201 Created** ✅ |

**Total: ~25 segundos** para criar um livro completo com author e genre novos!

## ✅ Vantagens no Contexto do `/create-complete`

1. **Garantia de Entrega:** Todos os eventos da SAGA são garantidamente entregues
2. **Consistência:** Se o BD salva, o evento será publicado - não há estados inconsistentes
3. **Resiliência:** SAGA continua mesmo se RabbitMQ ficar temporariamente indisponível
4. **Retry Automático:** Falhas transitórias são automaticamente resolvidas
5. **Auditoria:** Histórico completo de todos os passos da SAGA na tabela `outbox_events`
6. **Debugging Facilitado:** Podes ver exatamente onde a SAGA parou em caso de erro

## 🔍 Monitorização da SAGA

### Verificar eventos pendentes da SAGA atual:

```sql
-- Ver eventos pendentes
SELECT id, aggregate_type, event_type, aggregate_id, created_at, retry_count
FROM outbox_events 
WHERE processed = false
ORDER BY created_at;

-- Ver eventos de um book específico
SELECT * FROM outbox_events 
WHERE payload LIKE '%9780451524935%'
ORDER BY created_at;

-- Ver eventos que falharam
SELECT * FROM outbox_events 
WHERE retry_count > 0 AND processed = false;
```

### Verificar estado da SAGA:

```sql
-- Ver pending book requests
SELECT book_id, status, author_pending_received, genre_pending_received,
       author_finalized_received, genre_finalized_received, requested_at
FROM pending_book_request
WHERE status != 'BOOK_CREATED'
ORDER BY requested_at DESC;
```

## 🚀 Primeira Execução

Ao iniciar o microserviço pela primeira vez:
1. Hibernate criará automaticamente a tabela `outbox_events`
2. O `OutboxPublisher` começará a rodar automaticamente (graças ao `@EnableScheduling`)
3. Todos os eventos da SAGA serão guardados na Outbox antes de serem publicados

## 📊 Exchanges por Aggregate Type

O `OutboxPublisher` determina automaticamente o exchange correto:

| Aggregate Type | Exchange Name     | Routing Keys |
|----------------|-------------------|--------------|
| Book           | books.exchange    | BOOK_REQUESTED, BOOK_FINALIZED, BOOK_CREATED |
| Author         | authors.exchange  | AUTHOR_PENDING_CREATED, AUTHOR_CREATED |
| Genre          | genres.exchange   | GENRE_PENDING_CREATED, GENRE_CREATED |

## 🐛 Troubleshooting

### SAGA fica presa em `PENDING_AUTHOR_CREATION`

**Causa:** Eventos `AUTHOR_PENDING_CREATED` ou `GENRE_PENDING_CREATED` não foram publicados.

**Solução:**
```sql
-- Verificar se eventos estão na outbox
SELECT * FROM outbox_events WHERE event_type IN ('AUTHOR_PENDING_CREATED', 'GENRE_PENDING_CREATED') AND processed = false;

-- Se existirem, OutboxPublisher vai processá-los automaticamente
-- Se não existirem, houve um erro na criação - verificar logs
```

### Eventos com `bookId = null`

**Causa:** Publishers não estavam a incluir o `bookId` no payload.

**Solução:** Já corrigido! Os publishers agora fazem:
```java
authorViewAMQP.setBookId(bookId); // ✅ Incluído antes de serializar
```

### OutboxPublisher não está a correr

**Causa:** `@EnableScheduling` não está presente na classe main.

**Solução:**
```java
@SpringBootApplication
@EnableScheduling  // ✅ Necessário!
public class LMSBooks {
    public static void main(String[] args) {
        SpringApplication.run(LMSBooks.class, args);
    }
}
```

## 🔧 Manutenção

### Cleanup de eventos antigos:

```java
@Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
public void cleanupOldEvents() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    outboxEventRepository.deleteByProcessedTrueAndProcessedAtBefore(cutoff);
}
```

### Cleanup de pending requests antigos:

```sql
-- Limpar requests que falharam há mais de 7 dias
DELETE FROM pending_book_request 
WHERE status IN ('AUTHOR_CREATION_FAILED', 'GENRE_CREATION_FAILED')
  AND requested_at < NOW() - INTERVAL '7 days';
```

## 🎓 Conceitos Aprendidos

- **Transactional Outbox Pattern**
- **Eventual Consistency** (a SAGA demora ~25s mas garante consistência)
- **At-least-once delivery** (eventos podem ser duplicados, consumers devem ser idempotentes)
- **SAGA Pattern** (orchestration baseada em eventos)
- **Choreography** (cada bounded context reage a eventos autonomamente)

## 🎯 Próximos Passos

1. **Implementar idempotência** nos consumers (verificar se o evento já foi processado)
2. **Adicionar timeout** para SAGAs que demoram muito (> 5 minutos)
3. **Dashboard de monitorização** para acompanhar SAGAs em tempo real
4. **Dead Letter Queue** para eventos que falharam após max retries

---

**Status:** ✅ Implementado e testado em produção!

**Data de Implementação:** 29 de Dezembro de 2025

**Bugs Corrigidos:**
- ✅ Exchange names incorretos (`q.books.events` → `books.exchange`)
- ✅ `bookId` null nos eventos `AUTHOR_CREATED` e `GENRE_CREATED`
- ✅ Falta de `@Transactional` nos serviços
