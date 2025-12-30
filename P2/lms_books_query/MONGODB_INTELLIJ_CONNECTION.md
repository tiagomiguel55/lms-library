# Configuração MongoDB no IntelliJ - Ambiente DEV

## Informações de Conexão

### Para MongoDB Local (Docker)
Se você estiver rodando o MongoDB via Docker Compose localmente:

**Configurações:**
- **Host**: `localhost`
- **Port**: `27017`
- **Authentication Database**: `admin`
- **Username**: `admin`
- **Password**: `admin`
- **Database**: `query_books_dev`

**URL de Conexão:**
```
mongodb://admin:admin@localhost:27017/query_books_dev?authSource=admin
```

---

### Para MongoDB no Docker Swarm (VM remota)
Se você estiver rodando no Docker Swarm na sua VM Jenkins:

**Configurações:**
- **Host**: `<IP_DA_SUA_VM>` (ex: `192.168.x.x` ou IP público)
- **Port**: `27017`
- **Authentication Database**: `admin`
- **Username**: `admin`
- **Password**: `admin`
- **Database**: `query_books_dev`

**URL de Conexão:**
```
mongodb://admin:admin@<IP_DA_SUA_VM>:27017/query_books_dev?authSource=admin
```

---

## Como Configurar no IntelliJ IDEA

### Passo 1: Abrir o Database Tool Window
1. No IntelliJ, vá em **View** → **Tool Windows** → **Database** (ou pressione `Alt+1` e procure por "Database")
2. Ou use o atalho: clique no ícone "Database" no lado direito da janela

### Passo 2: Adicionar Nova Data Source
1. Clique no ícone `+` (Add)
2. Selecione **Data Source** → **MongoDB**

### Passo 3: Preencher as Informações

#### Aba "General":
- **Name**: `MongoDB Query DEV` (ou qualquer nome que preferir)
- **Host**: `localhost` (ou IP da VM se for remoto)
- **Port**: `27017`
- **Authentication**: `User & Password`
- **User**: `admin`
- **Password**: `admin`
- **Database**: `query_books_dev`
- **Auth database**: `admin`

#### URL Completa (se usar o campo URL):
```
mongodb://admin:admin@localhost:27017/query_books_dev?authSource=admin
```

### Passo 4: Download do Driver (se necessário)
- Se aparecer um aviso sobre "Missing Driver Files", clique em **Download**
- O IntelliJ vai baixar automaticamente o driver MongoDB

### Passo 5: Testar a Conexão
1. Clique em **Test Connection**
2. Se tudo estiver correto, você verá "Connection successful"
3. Clique em **OK** ou **Apply**

---

## Coleções Disponíveis no MongoDB

Após conectar, você verá as seguintes coleções:
- `books` - Livros do sistema
- `authors` - Autores
- `genres` - Gêneros literários
- `pending_book_events` - Eventos pendentes de livros (saga pattern)
- `photos` - Fotos/capas dos livros
- `forbidden_names` - Nomes proibidos

---

## Troubleshooting

### Erro: "Connection refused"
- Verifique se o MongoDB está rodando: `docker ps | grep mongo`
- Se não estiver, inicie: `docker-compose up -d mongodb_query_dev`

### Erro: "Authentication failed"
- Confirme as credenciais: username `admin`, password `admin`
- Confirme que o Authentication Database está configurado como `admin`

### Erro: "Timeout"
- Se conectando remotamente, verifique se a porta 27017 está aberta no firewall
- Verifique se consegue fazer telnet: `telnet <IP_VM> 27017`

### MongoDB não está rodando
Execute na pasta do projeto:
```bash
cd C:\Users\migue\IdeaProjects\lms-library\P2\lms_books_query
docker network create lms_network
docker-compose up -d mongodb_query_dev
```

---

## Queries Úteis para Testar

Após conectar, você pode executar queries diretamente no IntelliJ:

### Ver todos os livros
```javascript
db.books.find()
```

### Ver todos os autores
```javascript
db.authors.find()
```

### Ver gêneros
```javascript
db.genres.find()
```

### Ver eventos pendentes
```javascript
db.pending_book_events.find()
```

---

## Configuração Alternativa: MongoDB Compass

Se preferir usar o MongoDB Compass (ferramenta oficial):
1. Download: https://www.mongodb.com/try/download/compass
2. Use a connection string: `mongodb://admin:admin@localhost:27017/query_books_dev?authSource=admin`

