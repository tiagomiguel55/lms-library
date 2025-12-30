# Jenkins Pipeline - LMS AuthN and Users

Pipeline CI/CD simplificada para o microsserviço LMS AuthN and Users com deployment automático em Docker Swarm.

## 📋 Pré-requisitos

### Jenkins Plugins
- Git Plugin
- Docker Pipeline Plugin
- Email Extension Plugin (opcional)
- Pipeline Plugin

### Credenciais no Jenkins
Configure as seguintes credenciais no Jenkins:

1. **github-credentials**
   - Tipo: Username with password
   - ID: `github-credentials`
   - Username: seu username do GitHub
   - Password: Personal Access Token do GitHub

2. **dockerhub-credentials**
   - Tipo: Username with password
   - ID: `dockerhub-credentials`
   - Username: seu username do Docker Hub
   - Password: seu password/token do Docker Hub

## ⚙️ Configuração da Pipeline

### 1. Editar Variáveis de Ambiente no Jenkinsfile

Abra o `Jenkinsfile` e altere as seguintes variáveis:

```groovy
// Git
GIT_REPO_URL   = 'https://github.com/SEU_USERNAME/lms-authnusers.git'

// Docker Registry
DOCKER_REGISTRY_NAMESPACE = 'SEU_DOCKERHUB_USERNAME'

// Email
EMAIL_RECIPIENT = 'seu.email@gmail.com'
```

### 2. Criar Job no Jenkins

1. New Item → Pipeline
2. Nome: `lms-authnusers-pipeline`
3. Em "Pipeline":
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: (seu repositório)
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`

4. Em "Build Triggers" (opcional):
   - ☑️ Poll SCM: `H/5 * * * *` (verifica a cada 5 minutos)

5. Salvar

## 🚀 Como Usar a Pipeline

### Executar Build e Deploy

1. Acesse o job no Jenkins
2. Clique em "Build Now"
3. A pipeline executará automaticamente todos os stages

## 🔄 Fluxo da Pipeline

```
Checkout → Build & Package → Run Tests → Build Docker Image → 
Push to Registry → Initialize Swarm → Deploy to Swarm → Verification
```

## 🏗️ Stages da Pipeline

### 1. Checkout
Faz clone do repositório Git

### 2. Build & Package
Compila o projeto Maven
```bash
mvn clean package -DskipTests
```

### 3. Run Tests
Executa os testes unitários e publica relatórios JUnit
```bash
mvn test
```

### 4. Build Docker Image
Cria a imagem Docker com tag do build number e latest

### 5. Push Docker Image to Registry
Faz push da imagem para Docker Hub

### 6. Initialize Docker Swarm
Inicializa o Docker Swarm e cria a rede overlay `lms_network`

### 7. Deploy to Docker Swarm
Deploy da aplicação usando o `docker-compose-swarm.yml` existente

### 8. Post-Deployment Verification
Verifica o status dos serviços após deployment

## 📊 Arquitetura de Deployment

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Swarm                          │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  Stack: lmsauthnusers                                    │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │  PostgreSQL (1 replica)                          │   │
│  │  - Port: 5432                                    │   │
│  │  - Volume: postgres_data                         │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │  RabbitMQ (1 replica)                            │   │
│  │  - Port: 5672 (AMQP)                             │   │
│  │  - Port: 15672 (Management UI)                   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │  lmsauthnusers (2 replicas)                      │   │
│  │  - Port: 8090                                    │   │
│  │  - Load Balancer (Swarm Ingress)                 │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
│  Network: lms_network (overlay)                          │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## 🌐 Acesso aos Serviços

| Serviço | URL/Port | Descrição |
|---------|----------|-----------|
| **Application** | http://localhost:8090 | API REST |
| **Swagger UI** | http://localhost:8090/swagger-ui | Documentação interativa |
| **PostgreSQL** | localhost:5432 | Base de dados |
| **RabbitMQ** | localhost:5672 | Message broker |
| **RabbitMQ Management** | http://localhost:15672 | Interface de gestão (guest/guest) |

## 🔍 Monitorização

### Ver Status dos Serviços
```bash
# Listar stacks
docker stack ls

# Ver serviços do stack
docker stack services lmsauthnusers

# Ver tarefas/replicas
docker stack ps lmsauthnusers

# Logs de um serviço
docker service logs lmsauthnusers_lmsauthnusers -f
docker service logs lmsauthnusers_postgres -f
docker service logs lmsauthnusers_rabbitmq -f
```

### Verificar Réplicas
```bash
# Ver quantas réplicas estão running
docker service ls --filter name=lmsauthnusers

# Escalar serviço (se necessário)
docker service scale lmsauthnusers_lmsauthnusers=3
```

## 🗑️ Remover Deployment

### Remover Stack Completo
```bash
docker stack rm lmsauthnusers
```

### Limpar Volumes (⚠️ Cuidado - apaga dados!)
```bash
docker volume rm lmsauthnusers_postgres_data
docker volume rm lmsauthnusers_uploaded_files_volume_1
```

## 📧 Notificações por Email

A pipeline envia emails em:
- ✅ **Success**: Build e deployment bem sucedidos
- ❌ **Failure**: Falha em algum stage

## 🛠️ Troubleshooting

### Pipeline falha no Build
```bash
# Verificar logs do Maven
mvn clean package -DskipTests
```

### Docker build falha
```bash
# Testar localmente
docker build -t lmsauthnusers:test .
```

### Serviço não inicia no Swarm
```bash
# Ver logs
docker service logs lmsauthnusers_lmsauthnusers --tail 100

# Ver tarefas que falharam
docker service ps lmsauthnusers_lmsauthnusers --no-trunc
```

### PostgreSQL não está acessível
```bash
# Verificar se o serviço está running
docker service ps lmsauthnusers_postgres

# Testar conexão
docker exec -it $(docker ps -q -f name=postgres) psql -U postgres -c "\l"
```

### RabbitMQ não está acessível
```bash
# Verificar status
docker service ps lmsauthnusers_rabbitmq

# Acessar Management UI
# http://localhost:15672
# Username: guest / Password: guest
```

## 🔐 Segurança

### Boas Práticas

1. **Nunca commitar credenciais** no código
2. **Usar Jenkins Credentials** para secrets
3. **Rotacionar tokens** periodicamente
4. **Limitar acesso** ao Jenkins
5. **Usar HTTPS** em produção

### Variáveis Sensíveis

Estas variáveis são injetadas pelo Jenkins de forma segura:
- `DOCKER_USERNAME` / `DOCKER_PASSWORD`
- Credenciais Git

## 🔄 Comunicação entre Microsserviços

O microsserviço está configurado para comunicar com outros microsserviços através da rede Docker Swarm `lms_network`:

```properties
# application-bootstrap.properties
microservices.books-command.url=http://lmsbooks-command:8080
microservices.books-query.url=http://lmsbooks-query:8080
```

**Nota:** Os nomes dos serviços (`lmsbooks-command`, `lmsbooks-query`) devem corresponder aos nomes definidos nos stacks Docker Swarm dos outros microsserviços.

## 📝 Logs e Auditoria

Todos os deployments são registados:
- Jenkins Build History
- Docker service events
- Email notifications

## 🚦 Configuração Atual

| Componente | Configuração |
|------------|--------------|
| **Réplicas App** | 2 |
| **Réplicas PostgreSQL** | 1 |
| **Réplicas RabbitMQ** | 1 |
| **App Port** | 8090 |
| **PostgreSQL Port** | 5432 |
| **RabbitMQ Port** | 5672 / 15672 |
| **Network** | lms_network (overlay) |

## 📚 Recursos Adicionais

- [Docker Swarm Documentation](https://docs.docker.com/engine/swarm/)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 👥 Suporte

Para problemas ou questões:
1. Verificar logs no Jenkins
2. Verificar logs dos serviços Docker
3. Consultar documentação

---

**Desenvolvido para o projeto LMS - Library Management System**
