# ☁️ Serverless To-Do List API (Java & AWS)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-1.9+-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-Cloud-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)

Este repositório contém o backend de uma aplicação de gerenciamento de tarefas (To-Do List) totalmente **Serverless**, construída com **Java 21** na AWS.

O projeto demonstra um fluxo de desenvolvimento profissional e uma arquitetura robusta, utilizando **Infraestrutura como Código (IaC)** com Terraform, modelagem **Single-Table Design** no DynamoDB, autenticação segura via **Cognito** e processamento assíncrono com **SQS e SES**.

---

## 🏗️ Arquitetura e Design

O sistema foi projetado seguindo os princípios de arquitetura orientada a eventos e microsserviços.

### Fluxo de Autenticação e API
1.  O cliente se autentica no **Amazon Cognito** e recebe um Token JWT (`IdToken`).
2.  O cliente faz requisições ao **API Gateway** enviando o token no cabeçalho.
3.  O **Cognito Authorizer** valida o token e injeta a identidade do usuário na requisição.
4.  O API Gateway roteia a requisição para a função **AWS Lambda** específica (padrão *Single Responsibility*).
5.  A Lambda interage com o **DynamoDB** usando chaves particionadas por usuário (`USER#uuid`) para garantir isolamento e performance.

### Fluxo Assíncrono (Relatórios)
Para operações pesadas, utilizamos arquitetura assíncrona para evitar timeouts e melhorar a UX:
1.  **API Gateway** recebe o pedido de relatório (`POST /reports`).
2.  **Lambda Gatilho** valida o usuário e envia uma mensagem para uma fila **SQS**.
3.  A API responde imediatamente ao usuário (`202 Accepted`).
4.  **Lambda Trabalhadora** (acionada pelo SQS) processa a mensagem em segundo plano:
    * Busca todos os dados do usuário no DynamoDB.
    * Gera um arquivo CSV hierárquico.
    * Faz upload do CSV para um bucket **S3**.
    * Gera uma **URL pré-assinada** (segura e temporária).
    * Envia um e-mail para o usuário via **Amazon SES** contendo o link de download.

## 🚀 Funcionalidades

### ✅ Gestão de Tarefas e Listas
- **Listas:** Criar, Listar e Deletar listas de tarefas.
- **Tarefas:** CRUD completo para tarefas dentro de listas específicas.
- **Hierarquia:** As tarefas são estritamente vinculadas a uma lista e a um usuário.

### 🔐 Segurança
- **Autenticação:** Cadastro, Confirmação e Login (SRP/Senha) via Cognito User Pools.
- **Autorização:** Todas as rotas protegidas por validação de Token JWT.
- **Dados:** Isolamento lógico de dados por usuário no banco de dados.

### ⚡ Performance & Dados
- **Single-Table Design:** Utilização de uma única tabela DynamoDB com chaves compostas (PK/SK) para otimizar custos e permitir queries complexas (ex: buscar lista e tarefas em uma única chamada, se necessário).
- **Modelagem:**
    - `PK`: `USER#{userId}`
    - `SK` (Lista): `LIST#{listId}`
    - `SK` (Tarefa): `LIST#{listId}#TASK#{taskId}`

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 21 (Amazon Corretto)
* **Build:** Apache Maven
* **Cloud:** AWS (Lambda, API Gateway, DynamoDB, Cognito, SQS, S3, SES)
* **IaC:** Terraform (Modularizado)
* **CI/CD:** GitHub Actions (Pipeline de Build, Testes Unitários e Planejamento de Infraestrutura)
* **Testes:** JUnit 5 & Mockito
* **Bibliotecas:**
    - `aws-lambda-java-events`: Tipagem de eventos AWS.
    - `aws-sdk-java-v2`: SDK oficial da AWS (Modular).
    - `dynamodb-enhanced`: Mapeamento de objetos para DynamoDB.
    - `gson`: Serialização JSON.
    - `opencsv`: Geração de arquivos CSV.

---

## 📦 Como Rodar o Projeto

### Pré-requisitos
* [Java 21 JDK](https://aws.amazon.com/corretto/)
* [Apache Maven](https://maven.apache.org/download.cgi)
* [Terraform CLI](https://developer.hashicorp.com/terraform/install)
* [AWS CLI](https://aws.amazon.com/cli/) configurado com credenciais.

### 1. Clone o repositório
```bash
git clone [https://github.com/slimafilipe/TODOLambdaJava.git](https://github.com/slimafilipe/TODOLambdaJava.git)
cd TODOLambdaJava
````

### 2\. Build da Aplicação

Compile o projeto e gere o pacote `.jar` (Uber-jar) para as Lambdas.

```bash
mvn clean package
```

### 3\. Deploy da Infraestrutura (Terraform)

```bash
# Inicialize o Terraform
terraform init

# Visualize o plano
terraform plan

# Aplique a infraestrutura
terraform apply
```

*Confirme com `yes` quando solicitado.*

> **⚠️ Importante sobre o SES:** Após o deploy, a AWS enviará um e-mail de verificação para o endereço definido como remetente. Você deve confirmar esse e-mail para que o envio de relatórios funcione (enquanto estiver na Sandbox do SES).

-----

## 🔌 Documentação da API

**Base URL:** Disponível no output do Terraform como `api_invoke_url`.
**Auth:** Header `Authorization: Bearer {IdToken}` obrigatório em todas as rotas.

### 📂 Listas (TaskLists)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| **POST** | `/lists` | Cria uma nova lista. Body: `{ "listName": "..." }` |
| **GET** | `/lists` | Retorna todas as listas do usuário. |
| **GET** | `/lists/{listId}` | Retorna uma lista específica pelo ID. |
| **DELETE** | `/lists/{listId}` | Deleta uma lista e todas as suas tarefas. |

### ✅ Tarefas (Tasks)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| **POST** | `/lists/{listId}/tasks` | Cria tarefa na lista. Body: `{ "title": "...", "description": "..." }` |
| **GET** | `/lists/{listId}/tasks` | Lista todas as tarefas de uma lista específica. |
| **GET** | `/lists/{listId}/tasks/{taskId}` | Busca uma tarefa específica por ID. |
| **PUT** | `/lists/{listId}/tasks/{taskId}` | Atualiza uma tarefa. Body: `{ "title": "...", "completed": true }` |
| **DELETE** | `/lists/{listId}/tasks/{taskId}` | Deleta uma tarefa. |

### 📊 Relatórios

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| **POST** | `/reports` | Inicia a geração assíncrona do relatório CSV e envio por e-mail. |

-----

## 🧪 Testes

O projeto possui uma suíte de testes unitários cobrindo os Handlers e a lógica de negócio, utilizando Mocks para isolar a dependência da nuvem.

Para rodar os testes:

```bash
mvn test
```

-----

## 📂 Estrutura de Pastas

```text
/
├── .github/workflows/    # Pipelines de CI/CD
├── src/
│   ├── main/java/dev/filipe/TODOLambdaJava/
│   │   ├── config/       # Dependency Injection (Singleton Factory)
│   │   ├── controller/   # Lambda Handlers (Entrada da API)
│   │   │   ├── task/     # Handlers de Tarefas
│   │   │   ├── taskList/ # Handlers de Listas
│   │   │   └── queue/    # Handlers de SQS (Relatórios)
│   │   ├── dto/          # Data Transfer Objects (Records)
│   │   ├── model/        # Entidades do DynamoDB
│   │   ├── repository/   # Acesso a dados (DynamoDB Enhanced)
│   │   └── util/         # Utilitários (Auth, API Response, Mappers)
│   └── test/             # Testes Unitários
├── tf_modules/           # Módulos Terraform (Lambda, DynamoDB)
├── main.tf               # Definição da Infraestrutura Principal
└── pom.xml               # Dependências Maven
```

-----

## 👨‍💻 Autor

Desenvolvido por [Filipe Lima](https://www.google.com/search?q=https://github.com/slimafilipe).

```
```
