
# TODO Lambda AWS usando Java

Repósitorio do projeto de API construído com AWS Lambda e Java. O objetivo é demonstrar um fluxo de desenvolvimento completo, incluindo testes unitários com JUnit e Mockito, e um pipeline de CI automatizado com GitHub Actions.

## Instalação

## Pré-requisitos
Antes de começar, garanta que você tenha as seguintes ferramentas instaladas e configuradas:

* [Java 21 (Amazon Corretto)](https://aws.amazon.com/corretto/)
* [Apache Maven](https://maven.apache.org/download.cgi)

Clone o repositório

```bash
  git clone https://github.com/slimafilipe/TODOLambdaJava.git
```
Crie a branch dev
```bash
    git checkout dev
```
Instale as dependências
```bash
    mvn install
```

## Rodando os testes

Para rodar os testes, rode o seguinte comando

```bash
  mvn test
```

## Funcionalidades futuras
- [ ] Criar funcionalidedades CRUD completas para uma lista de tarefas
- [ ] Persistência usando DynamoDB
- [ ] Implementar autenticação e autorização
- [ ] Adicionar mais endpoints à API
- [ ] Melhorar a documentação da API
- [ ] Implementar monitoramento e logging avançado
- [ ] Adicionar mais testes unitários e de integração
- [ ] Configurar deploy automático para ambientes de staging e produção
- [ ] Adicionar suporte a múltiplos ambientes (dev, staging, prod)

## Stacks
![My Skills](https://skillicons.dev/icons?i=java&theme=light)
![My Skills](https://skillicons.dev/icons?i=maven&theme=light)
![My Skills](https://skillicons.dev/icons?i=aws&theme=light)
![My Skills](https://skillicons.dev/icons?i=githubactions&theme=light)
![My Skills](https://skillicons.dev/icons?i=dynamodb&theme=light)
![My Skills](https://skillicons.dev/icons?i=lambda&theme=light)
![My Skills](https://skillicons.dev/icons?i=apigateway&theme=light)
![My Skills](https://skillicons.dev/icons?i=cloudwatch&theme=light)
