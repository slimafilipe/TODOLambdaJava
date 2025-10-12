provider "aws" {
  region = "sa-east-1"
}

# --------------------------------------------
#----- Configuração do Amazon Cognito ------
# ---------------------------------------------

resource "aws_cognito_user_pool" "user_pool" {
  name = "task-user-pool"

  schema {
    attribute_data_type = "String"
    name                = "email"
    mutable             = false
    required            = true
  }
  password_policy {
    minimum_length    =  8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = false
  }
  username_attributes = ["email"]

  tags = {
    Project = "TODOLambdaJava"
  }
}

resource "aws_cognito_user_pool_client" "app_client" {
  name         = "tasks-app-client"
  user_pool_id = aws_cognito_user_pool.user_pool.id
  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
}

resource "aws_api_gateway_authorizer" "cognito_authorizer" {
  name        = "TaskApiCognitoAuthorizer"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "COGNITO_USER_POOLS"
  provider_arns = [aws_cognito_user_pool.user_pool.arn]
}

# ----- Módulo DynamoDB ------
module "todo_table" {
  source = "./tf_modules/dynamodb"

  table_name = "Tasks"
  hash_key_name = "userId"
  hash_key_type = "S"
  range_key_name = "taskId"
  range_key_type = "S"

    tags = {
      project = "TODOLambdaJava"
      ManagedBy   = "Terraform"
    }
}

resource "aws_iam_policy" "lambda_dynamodb_write_policy" {
  name = "lambda-dynamodb-tasks-write-policy"
  description = "Policy to allow Lambda functions to access DynamoDB table Tasks"
  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem"
        ],
        "Resource": module.todo_table.table_arn
      }
    ]
  })
}

resource "aws_iam_policy" "lambda_dynamodb_read_policy" {
  name = "lambda-list-tasks-dynamodb-policy"
    description = "Policy to allow Lambda functions to read from DynamoDB table Tasks"
  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:Scan",
          "dynamodb:Query",
          "dynamodb:GetItem"
        ],
        "Resource": module.todo_table.table_arn
      }
    ]
  })
}


# ----- Módulo Lambda ------



module "CreateTaskLambda" {
    source = "./tf_modules/lambda"

    function_name     = "create-task-lambda-java"
    handler           = "dev.filipe.TODOLambdaJava.controller.CreateTaskHandler::handleRequest"
    runtime           = "java21"
    source_code_path  = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
    memory_size       = 1024
    timeout           = 60
    tasks_table_name = module.todo_table.table_name
    tags = {
      Project   = "TODOLambdaJava"
      ManagedBy = "Terraform"
    }
}

module "ListTasksLambda" {
  source = "./tf_modules/lambda"

  function_name = "list-tasks-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.controller.ListTasksHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
    memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

module "UpdateTaskLambda" {
  source = "./tf_modules/lambda"
  function_name = "update-task-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.Controller.cpdateTaskHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

module "DeleteTaskLambda" {
  source = "./tf_modules/lambda"
  function_name = "delete-task-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.controller.DeleteTaskHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

module "GetTaskByIdLambda" {
  source = "./tf_modules/lambda"
  function_name = "get-task-by-id-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.controller.GetTaskByIdHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

resource "aws_api_gateway_rest_api" "task_api" {
  name = "APITASKS"
}
resource "aws_api_gateway_resource" "tasks_resource" {
  parent_id   = aws_api_gateway_rest_api.task_api.root_resource_id
  path_part   = "tasks"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "task_id_resource" {
  parent_id   = aws_api_gateway_resource.tasks_resource.id
  path_part   = "{taskId}"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}


resource "aws_api_gateway_method" "post_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "POST"
  resource_id   = aws_api_gateway_resource.tasks_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "post_task_integration" {
  http_method = aws_api_gateway_method.post_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.tasks_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.CreateTaskLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_tasks_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.tasks_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_task_integration" {
  http_method = aws_api_gateway_method.get_tasks_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.tasks_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.ListTasksLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_tasks_by_id_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.tasks_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_task_by_id_integration" {
  http_method = aws_api_gateway_method.get_tasks_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.tasks_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.ListTasksLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "update_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "PUT"
  resource_id   = aws_api_gateway_resource.task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "update_task_integration" {
  http_method = aws_api_gateway_method.update_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.UpdateTaskLambda.lambda_function_invoke_arn
}
resource "aws_api_gateway_method" "delete_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "DELETE"
  resource_id   = aws_api_gateway_resource.task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "delete_task_integration" {
  http_method = aws_api_gateway_method.delete_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.DeleteTaskLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.task_api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.tasks_resource.id,
      aws_api_gateway_resource.task_id_resource.id,
      aws_api_gateway_method.post_task_method.id,
      aws_api_gateway_integration.post_task_integration.id,
      aws_api_gateway_method.get_tasks_method.id,
      aws_api_gateway_integration.get_task_integration.id,
      aws_api_gateway_method.update_task_method.id,
      aws_api_gateway_integration.update_task_integration.id,
      aws_api_gateway_method.delete_task_method.id,
      aws_api_gateway_integration.delete_task_integration.id,
      aws_api_gateway_method.get_tasks_by_id_method.id,
      aws_api_gateway_integration.get_task_by_id_integration.id,
    ]))
  }
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "api_stage" {
  deployment_id = aws_api_gateway_deployment.api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
  stage_name    = "v2"
}

# ----- Permissões de Invocação da API Gateway -----

resource "aws_lambda_permission" "allow_api_gateway_create" {
  statement_id  = "AllowAPIGatewayInvokeCreate"
  action        = "lambda:InvokeFunction"
  function_name = module.CreateTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.post_task_method.http_method}${aws_api_gateway_resource.tasks_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_list" {
  statement_id  = "AllowAPIGatewayInvokeList"
  action        = "lambda:InvokeFunction"
  function_name = module.ListTasksLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_tasks_method.http_method}${aws_api_gateway_resource.tasks_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_list" {
  statement_id  = "AllowAPIGatewayInvokeList"
  action        = "lambda:InvokeFunction"
  function_name = module.GetTaskByIdLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_tasks_by_id_method.http_method}${aws_api_gateway_resource.tasks_resource.path}"
}


resource "aws_lambda_permission" "allow_api_gateway_update" {
  statement_id  = "AllowAPIGatewayInvokeUpdate"
  action        = "lambda:InvokeFunction"
  function_name = module.UpdateTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.update_task_method.http_method}${aws_api_gateway_resource.task_id_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_delete" {
  statement_id  = "AllowAPIGatewayInvokeDelete"
  action        = "lambda:InvokeFunction"
  function_name = module.DeleteTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.delete_task_method.http_method}${aws_api_gateway_resource.task_id_resource.path}"
}


resource "aws_iam_role_policy_attachment" "create_lambda_dynamodb_access" {
  role = module.CreateTaskLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "list_lambda_dynamodb_read_access" {
  role = module.ListTasksLambda.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

resource "aws_iam_role_policy_attachment" "get_by_id_lambda_dynamodb_read_access" {
  role = module.GetTaskByIdLambda.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

resource "aws_iam_role_policy_attachment" "update_lambda_dynamo_acess" {
  role       = module.UpdateTaskLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "delete_lambda_dynamo_acess" {
  role       = module.DeleteTaskLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

output "nome_da_tabela" {
  value = module.todo_table.table_name
}
output "arn_da_create_lambda" {
  description = "O ARN da função Lambda de criação de tarefas"
  value = module.CreateTaskLambda.lambda_function_arn
}

output "arn_da_list_lambda" {
    description = "O ARN da função Lambda de listagem de tarefas"
    value = module.ListTasksLambda.lambda_function_arn
}

output "arn_da_get_by_id_lambda" {
  description = "O ARN da função Lambda de listagem de tarefas por id"
  value = module.GetTaskByIdLambda.lambda_function_arn
}

output "arn_da_update_lambda" {
  description = "O ARN da função Lambda para atualização de tarefas"
  value = module.UpdateTaskLambda.lambda_function_arn
}
output "arn_delete_lambda" {
  description = "O ARN da função Lambda para deletar tarefas"
  value = module.DeleteTaskLambda.lambda_function_arn
}

