# Saga Pattern Implementation - Author Persistence Only After Book Creation

## ✅ Implementação Completa

### 🎯 Objetivo
Garantir que o autor **só é persistido na base de dados** quando o livro for **definitivamente criado com sucesso**.

## 🏗️ Arquitetura Implementada

### 1. **Nova Entidade: PendingAuthor**
```java
@Entity
@Table(name = "PendingAuthor")
public class PendingAuthor {
    private Long id;
    private String authorName;
    private String bookId; // ISBN
    private LocalDateTime createdAt;
    private Status status; // PENDING_BOOK_CREATION, BOOK_CREATED, AUTHOR_PERSISTED, FAILED
}
```

**Propósito:** Armazenar temporariamente informações do autor até o livro ser criado.

### 2. **Novo Repository: PendingAuthorRepository**
```java
public interface PendingAuthorRepository extends JpaRepository<PendingAuthor, Long> {
    Optional<PendingAuthor> findByBookId(String bookId);
    Optional<PendingAuthor> findByAuthorName(String authorName);
}
```

## 🔄 Fluxo Completo (Saga Pattern)

### **Passo 1: Cliente faz pedido**
```
POST /api/books/create-complete
{
  "bookId": "9780134685991",
  "authorName": "Joshua Bloch",
  "genreName": "Programming"
}
```

### **Passo 2: BOOK_REQUESTED recebido**
```java
// AuthorRabbitmqController.receiveBookRequested()

// Verificar se autor já existe
List<Author> existingAuthors = authorRepository.searchByNameName(authorName);

if (existingAuthors.isEmpty()) {
    // NOVO AUTOR - NÃO persistir ainda!
    PendingAuthor pendingAuthor = new PendingAuthor(authorName, bookId);
    pendingAuthorRepository.save(pendingAuthor); // ✅ Salva em tabela temporária
    
    // Envia evento com authorId = 0 (temporário)
    authorEventsPublisher.sendAuthorPendingCreated(0L, bookId, authorName, genreName);
} else {
    // AUTOR EXISTENTE - usa o ID real
    Author author = existingAuthors.get(0);
    authorEventsPublisher.sendAuthorPendingCreated(author.getAuthorNumber(), ...);
}
```

**✅ AUTOR NÃO ESTÁ NA TABELA `AUTHOR` AINDA!**

### **Passo 3: AUTHOR_PENDING_CREATED recebido**
```java
// BookRabbitmqController.receiveAuthorPendingCreated()

if (event.getAuthorId() != 0L) {
    // Autor existente - busca da BD
    author = authorRepository.findByAuthorNumber(event.getAuthorId());
} else {
    // Novo autor - cria objeto temporário (NÃO salva!)
    author = new Author(event.getAuthorName(), "Temporary author", null);
    // NÃO chama authorRepository.save()!
}

// Cria o livro
Book newBook = new Book(isbn, title, description, genre, authors, null);
bookRepository.save(newBook); // ✅ Livro criado com sucesso
```

**✅ LIVRO CRIADO COM SUCESSO!**

### **Passo 4: BOOK_FINALIZED publicado**
```java
// BookRabbitmqController.processPendingRequest()

Long authorId = (author.getAuthorNumber() == 0) ? 0L : author.getAuthorNumber();
bookService.publishBookFinalized(authorId, author.getName(), isbn);
```

### **Passo 5: BOOK_FINALIZED recebido - AUTOR FINALMENTE PERSISTIDO!**
```java
// AuthorRabbitmqController.receiveBookFinalized()

if (event.getAuthorId() == 0L) {
    // NOVO AUTOR - AGORA SIM persistir!
    
    // 1. Busca da tabela temporária
    PendingAuthor pendingAuthor = pendingAuthorRepository.findByBookId(bookId);
    
    // 2. ✅ CRIA O AUTOR REAL NA BASE DE DADOS
    Author author = new Author(pendingAuthor.getAuthorName(), "Bio for " + name, null);
    author = authorRepository.save(author); // ⭐ PERSISTIDO AQUI!
    
    System.out.println("✅ AUTHOR PERSISTED: " + author.getName() + " (ID: " + author.getAuthorNumber() + ")");
    
    // 3. Atualiza status
    pendingAuthor.setStatus(AUTHOR_PERSISTED);
    
    // 4. Marca como finalizado
    authorService.markAuthorAsFinalized(author.getAuthorNumber());
    
    // 5. Publica AUTHOR_CREATED com ID real
    authorEventsPublisher.sendAuthorCreated(author, event.getBookId());
}
```

**✅ AUTOR AGORA ESTÁ NA TABELA `AUTHOR`!**

## 📊 Tabelas na Base de Dados

### Durante o Processo:
```
PendingAuthor (temporária)
├── id: 1
├── authorName: "Joshua Bloch"
├── bookId: "9780134685991"
├── status: PENDING_BOOK_CREATION
└── createdAt: 2025-01-09 10:00:00

Book (criado)
├── isbn: "9780134685991"
├── title: "Book by Joshua Bloch"
└── ...

Author (ainda NÃO existe!)
└── (vazia)
```

### Após BOOK_FINALIZED:
```
Author (AGORA existe!)
├── author_number: 123
├── name: "Joshua Bloch"
├── bio: "Bio for Joshua Bloch"
├── finalized: true
└── ...

PendingAuthor (atualizada)
├── status: AUTHOR_PERSISTED ✅
└── ...
```

## ✅ Garantias Implementadas

1. **Autor só é persistido APÓS livro ser criado com sucesso**
2. **Se livro falhar, autor NÃO é criado** (evita dados órfãos)
3. **Tabela PendingAuthor mantém histórico** (auditoria)
4. **Suporta autores existentes** (authorId != 0)
5. **Transaction safety** com JPA

## 🚀 Pronto para Testar!

A implementação está completa. Quando executar a aplicação:

```sql
-- Verificar autores pendentes
SELECT * FROM pending_author;

-- Verificar autores reais (só aparecem APÓS livro criado)
SELECT * FROM author;

-- Verificar livros
SELECT * FROM book;
```

**O autor SÓ aparece na tabela `author` quando o livro for criado com sucesso!** ✅

