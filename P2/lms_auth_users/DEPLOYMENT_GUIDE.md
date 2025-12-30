# LMS Auth & Users - CI/CD e Deploy Guide

## Visão Geral

O microserviço `lms-authnusers` está configurado para fazer CI/CD e deploy automático via Jenkins, da mesma forma que o `lms_books_command`.

## Arquitetura de Deploy

```
GitHub → Jenkins → Docker Registry (Docker Hub) → Docker Swarm (VM)
```

## Ambientes Disponíveis

1. **Dev** - Porta 8084
2. **Staging** - Porta 8085  
3. **Production** - Porta 8086

Cada ambiente tem sua própria base de dados PostgreSQL.

## Pipeline Jenkins

### Stages da Pipeline

1. **Checkout** - Clona o repositório do GitHub
2. **Build & Package** - Compila o projeto com Maven
3. **Build Docker Image** - Cria imagem Docker
4. **Push to Registry** - Envia para Docker Hub (dev/staging)
5. **Deploy to Swarm** - Deploy no ambiente selecionado

### Como Executar a Pipeline

1. Acesse Jenkins: `http://<jenkins-url>:8080`
2. Selecione o job `lms-authnusers`
3. Clique em "Build with Parameters"
4. Escolha o ambiente:
   - `dev` - Deploy em desenvolvimento
   - `staging` - Deploy em staging
   - `prod` - Deploy em produção
5. Clique em "Build"

### Parâmetros da Pipeline

- **ENVIRONMENT**: `dev`, `staging`, ou `prod`
- **SKIP_DEPLOY**: `true` para apenas build sem deploy

## Deploy Manual (sem Jenkins)

### 1. Build da imagem Docker

```bash
cd lms-authnusers
mvn clean package -DskipTests
docker build -t lmsauthnusers:latest -f DockerfileWithPackaging .
```

### 2. Tag e Push para Docker Hub

```bash
docker tag lmsauthnusers:latest tiagomiguel55/lmsauthnusers:latest
docker push tiagomiguel55/lmsauthnusers:latest
```

### 3. Deploy no Docker Swarm

**Dev:**
```bash
export IMAGE_TAG=latest
docker stack deploy -c docker-compose-swarm.yml lmsauthnusers-dev --with-registry-auth
```

**Staging:**
```bash
export IMAGE_TAG=latest
docker stack deploy -c docker-compose-swarm-staging.yml lmsauthnusers-staging --with-registry-auth
```

**Production:**
```bash
export IMAGE_TAG=latest
docker stack deploy -c docker-compose-swarm-prod.yml lmsauthnusers-prod --with-registry-auth
```

## Verificação do Deploy

### 1. Verificar serviços rodando
```bash
docker service ls | grep lmsauthnusers
```

### 2. Ver logs
```bash
docker service logs -f lmsauthnusers-dev_lmsauthnusers
```

### 3. Testar endpoint
```bash
curl http://<vm-ip>:8084/api/public/login
```

## Estrutura de Rede

- **Dev**: Porta 8084, PostgreSQL 5434
- **Staging**: Porta 8085, PostgreSQL 5435
- **Production**: Porta 8086, PostgreSQL 5436

Todos os ambientes estão conectados à rede `lms_network` para comunicação com outros microserviços (BookCmd, etc.)

## Rollback

Para fazer rollback para uma versão anterior:

```bash
# Ver versões disponíveis
docker images tiagomiguel55/lmsauthnusers

# Deploy da versão antiga
export IMAGE_TAG=<commit-sha-anterior>
docker stack deploy -c docker-compose-swarm.yml lmsauthnusers-dev --with-registry-auth
```

## Configuração Jenkins

### Credenciais Necessárias

1. **GitHub**: `password_for_github_tiago`
2. **Docker Hub**: `dockerhub-credentials`

### Webhooks (Opcional)

Para build automático ao fazer push:

1. GitHub → Settings → Webhooks
2. Add webhook: `http://<jenkins-url>:8080/github-webhook/`
3. Selecione "Just the push event"

## Troubleshooting

### Build falha no Jenkins
```bash
# Verificar logs do Jenkins
# Verificar se Maven e Docker estão instalados no Jenkins agent
```

### Serviço não inicia
```bash
# Ver logs detalhados
docker service logs lmsauthnusers-dev_lmsauthnusers --tail 100

# Verificar saúde do serviço
docker service ps lmsauthnusers-dev_lmsauthnusers
```

### Imagem não faz pull
```bash
# Login no Docker Hub
docker login docker.io

# Pull manual
docker pull tiagomiguel55/lmsauthnusers:latest
```

## Integração com BookCmd

O `lms-authnusers` fornece autenticação JWT para o `lms_books_command`. Ambos usam:
- Mesmas chaves RSA (`rsa.public.key`, `rsa.private.key`)
- Mesma rede Docker (`lms_network`)
- Formato de JWT compatível

## Fluxo de Autenticação

### 1. Login (obter token JWT)
```bash
POST http://<vm-ip>:8084/api/public/login
Content-Type: application/json

{
  "username": "librarian@example.com",
  "password": "password123"
}
```

**Resposta:**
```json
{
  "id": 1,
  "username": "librarian@example.com",
  "name": "Library Admin",
  "authorities": ["ROLE_LIBRARIAN"]
}
```
**Header:** `Authorization: <JWT_TOKEN>`

### 2. Usar o token para criar livro
```bash
POST http://<vm-ip>:8081/api/books/create-complete
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "title": "The Da Vinci Code",
  "authorName": "Dan Brown",
  "genreName": "Thriller"
}
```

## Endpoints Importantes

### Auth Service (porta 8084)
- **Login**: `POST /api/public/login`
- **Register**: `POST /api/public/register`
- **Health**: `GET /actuator/health`

### Books Command (porta 8081)
- **Create Book**: `POST /api/books/create-complete` (requer LIBRARIAN)
- **List Books**: `GET /api/books` (requer autenticação)

## Roles e Permissões

- **READER**: Pode fazer lendings e consultar livros
- **LIBRARIAN**: Pode criar livros, autores e géneros
- **ADMIN**: Acesso total

## Monitorização

Para monitorizar o serviço em produção:

```bash
# CPU e Memória
docker stats $(docker ps -q -f name=lmsauthnusers)

# Número de réplicas
docker service scale lmsauthnusers-prod_lmsauthnusers=5

# Ver todas as réplicas
docker service ps lmsauthnusers-prod_lmsauthnusers
```

## Estrutura dos Ambientes

| Ambiente | Porta API | Porta PostgreSQL | Réplicas | URL |
|----------|-----------|------------------|----------|-----|
| Dev      | 8084      | 5434            | 2        | http://vm-ip:8084 |
| Staging  | 8085      | 5435            | 2        | http://vm-ip:8085 |
| Production | 8086    | 5436            | 3        | http://vm-ip:8086 |

## Exemplo Completo de Uso

### 1. Registar um Librarian (primeira vez)
```bash
POST http://<vm-ip>:8084/api/public/register
Content-Type: application/json

{
  "username": "librarian@example.com",
  "password": "SecurePass123",
  "name": "John Library",
  "role": "LIBRARIAN"
}
```

### 2. Fazer Login
```bash
POST http://<vm-ip>:8084/api/public/login
Content-Type: application/json

{
  "username": "librarian@example.com",
  "password": "SecurePass123"
}
```

Copie o token JWT do header `Authorization` da resposta.

### 3. Criar Livro com Autor e Género
```bash
POST http://<vm-ip>:8081/api/books/create-complete
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "title": "1984",
  "authorName": "George Orwell",
  "genreName": "Dystopian Fiction"
}
```

### 4. Resultado Esperado
```json
{
  "message": "Book creation request accepted",
  "status": "PROCESSING",
  "title": "1984",
  "bookId": "9781234567890",
  "details": "The book will be created asynchronously..."
}
```

## Segurança

### Chaves RSA
As chaves RSA devem ser as mesmas em ambos os microserviços:
- `lms-authnusers/src/main/resources/rsa.public.key`
- `lms-authnusers/src/main/resources/rsa.private.key`
- `lms_books_command/src/main/resources/rsa.public.key`
- `lms_books_command/src/main/resources/rsa.private.key`

### Renovar Token
Os tokens JWT expiram após 10 horas. Após expiração, faça login novamente para obter um novo token.

## Troubleshooting Autenticação

### 401 Unauthorized
- Token JWT expirado → Faça login novamente
- Token inválido → Verifique se copiou o token completo
- Chaves RSA diferentes → Verifique se são idênticas nos dois serviços

### 403 Forbidden
- User não tem permissão → Verifique se tem role LIBRARIAN
- Endpoint protegido → Verifique SecurityConfig

### Token não valida
```bash
# Verificar se as chaves RSA são idênticas
diff lms-authnusers/src/main/resources/rsa.public.key \
     P2/lms_books_command/src/main/resources/rsa.public.key
```

