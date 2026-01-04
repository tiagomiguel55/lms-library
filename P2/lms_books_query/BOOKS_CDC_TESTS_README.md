# Testes CDC (Consumer Driven Contract) para lms_books_query

## Resumo
Foram criados testes CDC completos para o módulo `lms_books_query` baseados no padrão encontrado no módulo `lms_books_command`. Os testes cobrem o papel de consumidor de eventos para atualização do modelo de leitura (read model).

### ✅ TODOS OS EVENTOS DE LEITURA IDENTIFICADOS E COBERTOS

O módulo `lms_books_query` atua principalmente como **consumidor** de eventos para manter seu modelo de leitura sincronizado:

**EVENTOS CONSUMIDOS:**
1. **book_created** - Book criado por outro serviço (comando)
2. **book_updated** - Book atualizado por outro serviço (comando)
3. **book_deleted** - Book deletado por outro serviço (comando)
4. **author_created** - Author criado (para atualização do read model)
5. **genre_created** - Genre criado (para atualização do read model)
6. **book_finalized** - Book finalizado após processo de aprovação

**EVENTOS PRODUZIDOS:**
- O módulo books_query **não produz eventos** pois é puramente um serviço de consulta
- Apenas consome eventos para manter sua base de dados de leitura atualizada

## Estrutura de Testes

### 1. Testes de Definição de Contratos (Consumer)
**Arquivo:** `BooksCDCDefinitionTest.java`
- Define contratos para 6 tipos de eventos consumidos
- Gera arquivos JSON de contratos na pasta `target/pacts/`
- Foco nos eventos que o módulo query precisa processar

### 2. Testes de Integração do Consumidor
**Arquivo:** `BooksCDCConsumerIT.java`
- Testa o processamento de mensagens baseado nos contratos gerados
- Verifica se o `BookRabbitmqController` processa corretamente as mensagens
- Usa mocks adequados para todas as dependências
- Valida a atualização do read model

### 3. Testes do Produtor (Provider)
**Arquivos:** 
- `BooksProducerCDCIT.java` - Testes usando arquivos locais
- `BooksProducerFromPactBrokerCDCIT.java` - Testes usando Pact Broker

**Nota:** Como books_query é principalmente consumidor, os testes de produtor servem para verificar que o formato das mensagens está correto.

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

### Eventos Consumidos pelo Books Query

| Evento | Fonte | Processamento | Handler |
|--------|-------|---------------|---------|
| `book_created` | Books Command | Criar book no read model | `receiveBookCreatedMsg()` |
| `book_updated` | Books Command | Atualizar book no read model | `receiveBookUpdated()` |
| `book_deleted` | Books Command | Deletar book do read model | (Não implementado) |
| `author_created` | Author Service | Atualizar dados de author | (Para referência) |
| `genre_created` | Genre Service | Atualizar dados de genre | (Para referência) |
| `book_finalized` | Book Service | Finalizar book no read model | `receiveBookFinalized()` |

### Características do Books Query

| Aspecto | Detalhes |
|---------|----------|
| **Papel Principal** | **Consumidor** de eventos |
| **Responsabilidade** | Manter read model atualizado |
| **Base de Dados** | MongoDB (otimizada para leitura) |
| **Sincronização** | Eventual consistency |
| **Eventos Produzidos** | Nenhum (apenas consultas) |

## Configuração dos Testes

### Profile CDC
Os testes usam o profile `cdc-test` que:
- Mock do `RabbitTemplate` para evitar dependências externas
- Mock do `DirectExchange` 
- Configuração específica para CDC

### Dependências Mockadas
- `BookService` - Serviço de negócio para read model
- `BookEventsPublisher` - Publisher de eventos (se necessário)

## Validação

### Consumer Tests
- ✅ Verifica se o serviço consegue processar mensagens no formato esperado
- ✅ Valida mapeamento JSON para objetos
- ✅ Testa handlers de mensagens RabbitMQ
- ✅ Confirma atualização do read model

### Producer Tests  
- ✅ Verifica formato correto das mensagens (para compatibilidade)
- ✅ Valida serialização de objetos para JSON
- ✅ Garante consistência de contratos

## Benefícios

1. **Detecção Precoce de Quebras**: Mudanças em contratos são detectadas antes da produção
2. **Documentação Viva**: Contratos servem como documentação atualizada
3. **Testes Independentes**: Não dependem de serviços externos
4. **Cobertura do Read Model**: Todos os eventos de leitura estão cobertos
5. **Integração CI/CD**: Podem ser executados em pipelines automatizados
6. **Eventual Consistency**: Valida sincronização entre command e query

## Diferenças com Books Command

### Books Command (Write Side)
- **Produz** muitos eventos
- Processa comandos e workflows
- Base de dados normalizada (H2/PostgreSQL)
- Lógica de negócio complexa

### Books Query (Read Side)
- **Consome** eventos para leitura
- Otimizado para consultas
- Base de dados desnormalizada (MongoDB)
- Lógica simples de projeção

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
5. **Métricas**: Monitorar lag de sincronização entre command e query

## Manutenção

- **Atualizar contratos** quando houver mudanças nos eventos
- **Executar regularmente** para garantir compatibilidade
- **Versionar contratos** para gestão de mudanças
- **Revisar cobertura** quando novos eventos forem adicionados
- **Monitorar sincronização** entre write e read models

## Comandos Úteis

### Limpeza
```bash
# Limpar contratos gerados
rm -rf target/pacts/
```

### Debug
```bash
# Executar com logs detalhados
mvn test -Dtest="*CDC*" "-Dspring.profiles.active=cdc-test" -X
```

### Verificação de Arquivos
```bash
# Verificar se os contratos foram gerados
ls -la target/pacts/
```

---

*Documentação gerada em: 04/01/2026*  
*Baseado no padrão estabelecido em: lms_books_command*  
*Adaptado para o contexto de: lms_books_query (Read Model)*
