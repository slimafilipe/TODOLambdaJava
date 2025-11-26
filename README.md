# ☁️ Serverless To-Do List API (Java & AWS)

[](https://aws.amazon.com/corretto/)
[](https://www.terraform.io/)
[](https://aws.amazon.com/)
[](https://opensource.org/licenses/MIT)

Este repositório contém o backend de uma aplicação de gerenciamento de tarefas (To-Do List) totalmente **Serverless**, construída com **Java 21** na AWS.

O projeto demonstra um fluxo de desenvolvimento profissional e uma arquitetura robusta, utilizando **Infraestrutura como Código (IaC)** com Terraform, modelagem **Single-Table Design** no DynamoDB, autenticação segura via **Cognito** e processamento assíncrono com **SQS e SES**.

## 🏗️ Arquitetura

O projeto adota uma arquitetura orientada a eventos e microsserviços (funções Lambda):

\<p align="center"\>
\<img src="[https://skillicons.dev/icons?i=aws,lambda,apigateway,dynamodb,cognito,sqs,ses,s3,terraform\&theme=light](https://www.google.com/search?q=https://skillicons.dev/icons%3Fi%3Daws,lambda,apigateway,dynamodb,cognito,sqs,ses,s3,terraform%26theme%3Dlight)" alt="Tech Stack Diagram" /\>
\</p\>

### Componentes Principais

  * **Compute:** AWS Lambda (Java 21 com Amazon Corretto).
  * **API:** Amazon API Gateway (REST API com integração Proxy).
  * **Database:** Amazon DynamoDB (Single-Table Design para Listas e Tarefas).
  * **Auth:** Amazon Cognito User Pool (JWT Bearer Token).
  * **Async/Jobs:** Amazon SQS (Fila para processamento de relatórios em background).
  * **Storage/Mail:** Amazon S3 (Armazenamento de CSVs) e Amazon SES (Envio de e-mails).
  * **IaC:** Terraform.
  * **CI/CD:** GitHub Actions.

## 🚀 Funcionalidades

✅ **Funcionalidades Atuais:**

  * [x] **Autenticação Completa:** Cadastro de usuário, confirmação por código (e-mail) e login (retorna JWT).
  * [x] **Segurança:** API protegida por autorizador Cognito. Suporte a CORS habilitado.
  * [x] **Gestão de Listas:** CRUD completo para listas de tarefas.
  * [x] **Gestão de Tarefas:** CRUD completo para tarefas dentro de listas, usando chaves compostas (PK/SK) no DynamoDB.
  * [x] **Performance:** Modelagem Single-Table Design otimizada para queries rápidas por usuário.
  * [x] **Processamento Assíncrono:** Geração de relatórios pesados (CSV com todas as tarefas do usuário) feita em background via SQS + Lambda Worker, evitando timeouts na API.
  * [x] **Notificações:** Envio do link de download do relatório por e-mail via SES.
  * [x] **Infraestrutura Automatizada:** 100% dos recursos AWS provisionados via Terraform.

🔜 **Roadmap Futuro:**

  * [ ] Implementar testes de integração (E2E) rodando no pipeline de CI.
  * [ ] Adicionar dashboards avançados no CloudWatch e rastreamento com X-Ray.
  * [ ] Implementar WebSockets no API Gateway para atualizações em tempo real no frontend.

## 🛠️ Pré-requisitos

Antes de começar, garanta que você tenha as seguintes ferramentas instaladas e configuradas:

  * [Java 21 (Amazon Corretto)](https://aws.amazon.com/corretto/)
  * [Apache Maven](https://maven.apache.org/download.cgi)
  * [Terraform CLI](https://developer.hashicorp.com/terraform/install)
  * [AWS CLI](https://aws.amazon.com/cli/) (Configurado com credenciais de administrador: `aws configure`)

## 📦 Instalação e Deploy

### 1\. Clone o repositório

```bash
git clone https://github.com/slimafilipe/TODOLambdaJava.git
cd TODOLambdaJava
git checkout dev
```

### 2\. Build da Aplicação Java

Compile o projeto e gere o pacote `.jar` que será enviado para as Lambdas.

```bash
mvn clean package
```

*O sucesso deste passo gera o arquivo `target/todo-lambda-java-1.0-SNAPSHOT.jar`.*

### 3\. Provisionar Infraestrutura (Terraform)

O deploy é totalmente automatizado pelo Terraform.

```bash
# 1. Inicialize o Terraform (baixa plugins necessários)
terraform init

# 2. Visualize o plano de execução (opcional, mas recomendado)
terraform plan

# 3. Aplique a infraestrutura na AWS
terraform apply
```

*Confirme a execução digitando `yes` quando solicitado.*

> **Saídas do Terraform:** Ao final do comando `apply`, o Terraform exibirá informações cruciais como a URL base da API (`api_invoke_url`) e os IDs do Cognito. Anote-os para usar no frontend ou nos testes.

## 🧪 Rodando os Testes

O projeto utiliza **JUnit 5** e **Mockito** para testes unitários, focando na lógica de negócios e na camada de controle (Handlers) de forma isolada da nuvem.

```bash
mvn test
```

## 🔌 Documentação da API (Endpoints Principais)

**Base URL:** `https://{api_id}.execute-api.{region}.amazonaws.com/v2`
**Auth:** Todas as rotas (exceto cadastro/login) requerem header `Authorization: Bearer {id_token}`.

| Recurso | Método | Rota | Descrição |
| :--- | :--- | :--- | :--- |
| **Listas** | GET | `/lists` | Retorna todas as listas do usuário. |
| | POST | `/lists` | Cria uma nova lista. |
| | DELETE | `/lists/{listId}` | Deleta uma lista e suas tarefas. |
| **Tarefas** | GET | `/lists/{listId}/tasks` | Retorna tarefas de uma lista. |
| | POST | `/lists/{listId}/tasks` | Cria uma tarefa na lista. |
| | PUT | `/lists/{listId}/tasks/{taskId}`| Atualiza uma tarefa (ex: marcar como concluída). |
| | DELETE | `/lists/{listId}/tasks/{taskId}`| Deleta uma tarefa. |
| **Relatórios**| POST | `/reports` | Solicita a geração assíncrona do relatório CSV. |

## 📂 Estrutura do Projeto

```
/
├── .github/workflows/   # Pipelines de CI/CD (GitHub Actions)
├── src/
│   ├── main/java/dev/filipe/TODOLambdaJava/
│   │   ├── config/      # Injeção de dependências (Clientes AWS)
│   │   ├── controller/  # Handlers Lambda (Pontos de entrada da API e SQS)
│   │   ├── dto/         # Objetos de Transferência de Dados (Records)
│   │   ├── model/       # Entidades do DynamoDB
│   │   ├── repository/  # Camada de acesso a dados (DynamoDB Enhanced Client)
│   │   └── util/        # Utilitários (Auth, Respostas API, Mappers)
│   └── test/            # Testes Unitários com JUnit/Mockito
├── tf_modules/          # Módulos Terraform reutilizáveis
├── main.tf              # Arquivo principal da infraestrutura Terraform
└── pom.xml              # Gerenciamento de dependências Maven
```

## Stacks

\<div align="center"\>
\<img src="[https://skillicons.dev/icons?i=java,maven,aws,terraform,githubactions,dynamodb,lambda,apigateway,sqs,ses\&theme=light\&perline=10](https://www.google.com/search?q=https://skillicons.dev/icons%3Fi%3Djava,maven,aws,terraform,githubactions,dynamodb,lambda,apigateway,sqs,ses%26theme%3Dlight%26perline%3D10)" /\>
\</div\>
