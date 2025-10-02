provider "aws" {
  region = "sa-east-1"
  profile = "tf_windows_filipelima"
}

# ----- Módulo DynamoDB ------
module "todo_table" {
  source = "../tf_modules/dynamodb"

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
    source = "../tf_modules/lambda"

    function_name     = "create-task-lambda-java"
    handler           = "dev.filipe.TODOLambdaJava.Controller.CreateTaskHandler::handleRequest"
    runtime           = "java21"
    source_code_path  = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
    memory_size       = 1024
    timeout           = 60
    tasks_table_name = module.todo_table.table_name
    tags = {
      Project   = "TODOLambdaJava"
      ManagedBy = "Terraform"
    }
}

module "ListTasksLambda" {
  source = "../tf_modules/lambda"

  function_name = "list-tasks-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.Controller.ListTasksHandler::handleRequest"
  runtime = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
    memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

module "UpdateTaskLambda" {
  source = "../tf_modules/lambda"
  function_name = "update-task-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.Controller.UpdateTaskHandler::handleRequest"
  runtime = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

module "DeleteTaskLambda" {
  source = "../tf_modules/lambda"
  function_name = "delete-task-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.Controller.DeleteTaskHandler::handleRequest"
  runtime = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  tags = {
    Project = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

resource "aws_api_gateway_rest_api" "task_api" {
  name = "TasksAPi"
}
resource "aws_api_gateway_resource" "task_resource" {
  parent_id   = aws_api_gateway_rest_api.task_api.root_resource_id
  path_part   = "tasks"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "user_id_resource" {
  parent_id   = aws_api_gateway_resource.task_resource.id
  path_part   = "{userId}"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "task_id_resource" {
  parent_id   = aws_api_gateway_resource.task_resource.id
  path_part   = "{taskId}"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_method" "post_task_method" {
  authorization = "NONE"
  http_method   = "POST"
  resource_id   = aws_api_gateway_resource.task_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "post_task_integration" {
  http_method = aws_api_gateway_method.post_task_method.http_method
  resource_id = aws_api_gateway_resource.task_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.CreateTaskLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_tasks_method" {
  authorization = "NONE"
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.user_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_task_integration" {
  http_method = aws_api_gateway_method.get_tasks_method.http_method
  resource_id = aws_api_gateway_resource.user_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.ListTasksLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "update_task_method" {
  authorization = "NONE"
  http_method   = "PUT"
  resource_id   = aws_api_gateway_resource.task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "update_task_integration" {
  http_method = aws_api_gateway_method.update_task_method.http_method
  resource_id = aws_api_gateway_resource.task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.UpdateTaskLambda.lambda_function_invoke_arn
}
resource "aws_api_gateway_method" "delete_task_method" {
  authorization = "NONE"
  http_method   = "DELETE"
  resource_id   = aws_api_gateway_resource.task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "delete_task_integration" {
  http_method = aws_api_gateway_method.delete_task_method.http_method
  resource_id = aws_api_gateway_resource.task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.DeleteTaskLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.task_api.id

  depends_on = [
    aws_api_gateway_integration.get_task_integration,
    aws_api_gateway_integration.post_task_integration,
    aws_api_gateway_integration.update_task_integration,
    aws_api_gateway_integration.delete_task_integration
  ]
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "api_stage" {
  deployment_id = aws_api_gateway_deployment.api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
  stage_name    = "v1"
}


resource "aws_iam_role_policy_attachment" "create_lambda_dynamodb_access" {
  role = module.CreateTaskLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "list_lambda_dynamodb_read_access" {
  role = module.ListTasksLambda.iam_role_name

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

