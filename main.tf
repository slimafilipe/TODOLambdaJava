terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.99.1"
    }
  }
}
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

# ----- Módulo Lambdas TaskList -----
module "CreateTaskListLambda" {
  source = "./tf_modules/lambda"

  function_name     = "create-taskList-lambda-java"
  handler           = "dev.filipe.TODOLambdaJava.controller.taskList.CreateListHandler::handleRequest"
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
module "ListTaskListsLambda" {
  source = "./tf_modules/lambda"

  function_name     = "list-taskLists-lambda-java"
  handler           = "dev.filipe.TODOLambdaJava.controller.taskList.ListTaskListsHandler::handleRequest"
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
module "GetTaskListByIdLambda" {
  source = "./tf_modules/lambda"

  function_name     = "get-taskList-by-id-lambda-java"
  handler           = "dev.filipe.TODOLambdaJava.controller.taskList.GetTaskListByIdHandler::handleRequest"
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
module "DeleteTaskListLambda" {
  source = "./tf_modules/lambda"

  function_name     = "delete-taskList-lambda-java"
  handler           = "dev.filipe.TODOLambdaJava.controller.taskList.DeleteTaskListHandler::handleRequest"
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

# ----- Módulo Lambdas Tasks ------
module "CreateTaskLambda" {
    source = "./tf_modules/lambda"

    function_name     = "create-task-lambda-java"
    handler           = "dev.filipe.TODOLambdaJava.controller.task.CreateTaskHandler::handleRequest"
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
  handler = "dev.filipe.TODOLambdaJava.controller.task.ListTasksHandler::handleRequest"
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
  handler = "dev.filipe.TODOLambdaJava.controller.task.UpdateTaskHandler::handleRequest"
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
  handler = "dev.filipe.TODOLambdaJava.controller.task.DeleteTaskHandler::handleRequest"
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
  handler = "dev.filipe.TODOLambdaJava.controller.task.GetTaskByIdHandler::handleRequest"
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

# ----- Módulo Lambda SQS e SES ---------
module "StartReportLambda" {
  source = "./tf_modules/lambda"
  function_name = "start-report-lambda-java"
  handler = "dev.filipe.TODOLambdaJava.controller.queue.StartReportHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.todo_table.table_name
  enviroment_variables = {
    QUEUE_URL = aws_sqs_queue.report_queue.url
  }
  tags = { Project = "TODOLambdaJava"}
}

module "ProcessReportLambda" {
  source = "./tf_modules/lambda"
  function_name = "process-report-lambda-java"
  handler = "dev.filipe.TODOLambda.controller.queue.ProcessReportHandler::handleRequest"
  runtime = "java21"
  source_code_path = "./target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  tasks_table_name = module.todo_table.table_name
  timeout = 300
  enviroment_variables = {
    CSV_BUCKET_NAME = aws_s3_bucket.csv_bucket.id
    SENDER_NAME = aws_ses_email_identity.sender_email.email
  }
  tags = { Project = "TODOLambdaJava"}
}
resource "aws_lambda_event_source_mapping" "report_queue_trigger" {
  event_source_arn = aws_sqs_queue.report_queue.arn
  function_name = module.ProcessReportLambda.lambda_function_arn
  batch_size = 1
}

resource "aws_api_gateway_rest_api" "task_api" {
  name = "APITASKS"
  description = "API para Tasks e TaskLists - Atualizado em ${timestamp()}"
}
resource "aws_api_gateway_resource" "lists_resource" {
  parent_id   = aws_api_gateway_rest_api.task_api.root_resource_id
  path_part   = "lists"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "list_id_resource" {
  parent_id   = aws_api_gateway_resource.lists_resource.id
  path_part   = "{listId}"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "list_tasks_resource" {
  parent_id   = aws_api_gateway_resource.list_id_resource.id
  path_part   = "tasks"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "list_task_id_resource" {
  parent_id   = aws_api_gateway_resource.list_tasks_resource.id
  path_part   = "{taskId}"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_resource" "reports_resource" {
  parent_id   = aws_api_gateway_rest_api.task_api.root_resource_id
  path_part   = "reports"
  rest_api_id = aws_api_gateway_rest_api.task_api.id
}


# --- API METHOD TASKLISTS ---
resource "aws_api_gateway_method" "post_list_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "POST"
  resource_id   = aws_api_gateway_resource.lists_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "post_list_task_integration" {
  http_method = aws_api_gateway_method.post_list_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.lists_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.CreateTaskListLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_list_tasks_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.lists_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_list_tasks_integration" {
  http_method = aws_api_gateway_method.get_list_tasks_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.lists_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.ListTaskListsLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_list_task_by_id_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.list_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_list_task_by_id_integration" {
  http_method = aws_api_gateway_method.get_list_task_by_id_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.GetTaskByIdLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "delete_list_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "DELETE"
  resource_id   = aws_api_gateway_resource.list_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "delete_list_task_integration" {
  http_method = aws_api_gateway_method.delete_list_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.DeleteTaskListLambda.lambda_function_invoke_arn
}
#---------------------------


# --- API METHOD TASKS ---
resource "aws_api_gateway_method" "post_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "POST"
  resource_id   = aws_api_gateway_resource.list_tasks_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "post_task_integration" {
  http_method = aws_api_gateway_method.post_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_tasks_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.CreateTaskLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_tasks_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.list_tasks_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_task_integration" {
  http_method = aws_api_gateway_method.get_tasks_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_tasks_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.ListTasksLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "get_tasks_by_id_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "get_task_by_id_integration" {
  http_method = aws_api_gateway_method.get_tasks_by_id_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.GetTaskByIdLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_method" "update_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "PUT"
  resource_id   = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "update_task_integration" {
  http_method = aws_api_gateway_method.update_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.UpdateTaskLambda.lambda_function_invoke_arn
}
resource "aws_api_gateway_method" "delete_task_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method   = "DELETE"
  resource_id   = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "delete_task_integration" {
  http_method = aws_api_gateway_method.delete_task_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.list_task_id_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.DeleteTaskLambda.lambda_function_invoke_arn
}
# --------------

resource "aws_api_gateway_method" "post_report_method" {
  authorization = "COGNITO_USER_POOLS"
  authorizer_id   = aws_api_gateway_authorizer.cognito_authorizer.id
  http_method = "POST"
  resource_id   = aws_api_gateway_resource.reports_resource.id
  rest_api_id   = aws_api_gateway_rest_api.task_api.id
}
resource "aws_api_gateway_integration" "post_report_integration" {
  http_method = aws_api_gateway_method.post_report_method.http_method
  integration_http_method = "POST"
  resource_id = aws_api_gateway_resource.reports_resource.id
  rest_api_id = aws_api_gateway_rest_api.task_api.id
  type        = "AWS_PROXY"
  uri         = module.StartReportLambda.lambda_function_invoke_arn
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.task_api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.lists_resource.id,
      aws_api_gateway_resource.list_id_resource.id,

      aws_api_gateway_resource.list_tasks_resource.id,
      aws_api_gateway_resource.list_task_id_resource.id,

      aws_api_gateway_method.post_list_task_method.id,
      aws_api_gateway_integration.post_list_task_integration.id,
      aws_api_gateway_method.get_list_tasks_method.id,
      aws_api_gateway_integration.get_list_tasks_integration.id,
      aws_api_gateway_method.get_list_task_by_id_method.id,
      aws_api_gateway_integration.get_list_task_by_id_integration.id,
      aws_api_gateway_method.delete_list_task_method.id,
      aws_api_gateway_integration.delete_list_task_integration.id,

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

      aws_api_gateway_resource.reports_resource.id,
      aws_api_gateway_method.post_report_method.id,
      aws_api_gateway_integration.post_report_integration.id,
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
#----------------------------------------

# ----- Permissões de Invocação da API Gateway das Lambdas TaskLists ------
resource "aws_lambda_permission" "allow_api_gateway_create_list" {
  statement_id  = "AllowAPIGatewayInvokeCreateList"
  action        = "lambda:InvokeFunction"
  function_name = module.CreateTaskListLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.post_list_task_method.http_method}${aws_api_gateway_resource.lists_resource.path}"
}
resource "aws_lambda_permission" "allow_api_gateway_list_taskLists" {
  statement_id  = "AllowAPIGatewayInvokeListTaskLists"
  action        = "lambda:InvokeFunction"
  function_name = module.ListTaskListsLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_list_tasks_method.http_method}${aws_api_gateway_resource.lists_resource.path}"
}
resource "aws_lambda_permission" "allow_api_gateway_get_list_by_id" {
  statement_id  = "AllowAPIGatewayInvokeGetListById"
  action        = "lambda:InvokeFunction"
  function_name = module.GetTaskListByIdLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_list_task_by_id_method.http_method}${aws_api_gateway_resource.list_id_resource.path}"
}
resource "aws_lambda_permission" "allow_api_gateway_delete_list" {
  statement_id  = "AllowAPIGatewayInvokeDeleteList"
  action        = "lambda:InvokeFunction"
  function_name = module.DeleteTaskListLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.delete_list_task_method.http_method}${aws_api_gateway_resource.list_id_resource.path}"
}



# ----- Permissões de Invocação da API Gateway das Lambdas Tasks-----

resource "aws_lambda_permission" "allow_api_gateway_create" {
  statement_id  = "AllowAPIGatewayInvokeCreate"
  action        = "lambda:InvokeFunction"
  function_name = module.CreateTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.post_task_method.http_method}${aws_api_gateway_resource.list_tasks_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_list" {
  statement_id  = "AllowAPIGatewayInvokeList"
  action        = "lambda:InvokeFunction"
  function_name = module.ListTasksLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_tasks_method.http_method}${aws_api_gateway_resource.list_tasks_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_get_by_id" {
  statement_id  = "AllowAPIGatewayInvokeGetById"
  action        = "lambda:InvokeFunction"
  function_name = module.GetTaskByIdLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.get_tasks_by_id_method.http_method}${aws_api_gateway_resource.list_task_id_resource.path}"
}


resource "aws_lambda_permission" "allow_api_gateway_update" {
  statement_id  = "AllowAPIGatewayInvokeUpdate"
  action        = "lambda:InvokeFunction"
  function_name = module.UpdateTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.update_task_method.http_method}${aws_api_gateway_resource.list_task_id_resource.path}"
}

resource "aws_lambda_permission" "allow_api_gateway_delete" {
  statement_id  = "AllowAPIGatewayInvokeDelete"
  action        = "lambda:InvokeFunction"
  function_name = module.DeleteTaskLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.delete_task_method.http_method}${aws_api_gateway_resource.list_task_id_resource.path}"
}

# ---- IAM Lambdas TaskList ----
resource "aws_iam_role_policy_attachment" "create_list_lambda_dynamodb_access" {
  role = module.CreateTaskListLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}
resource "aws_iam_role_policy_attachment" "list_taskList_lambda_dynamodb_read_access" {
  role = module.ListTaskListsLambda.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}
resource "aws_iam_role_policy_attachment" "get_list_by_id_lambda_dynamodb_read_access" {
  role = module.GetTaskListByIdLambda.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}
resource "aws_iam_role_policy_attachment" "delete_list_lambda_dynamo_acess" {
  role       = module.DeleteTaskListLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

# ---- IAM Lambdas Task ---
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

resource "aws_iam_role_policy_attachment" "start_report_lambda_sqs_acess" {
  role = module.StartReportLambda.iam_role_name
  policy_arn = aws_iam_policy.sqs_send_policy.arn
}
resource "aws_iam_role_policy_attachment" "process_report_lambda_worker_access" {
  role = module.ProcessReportLambda.iam_role_name
  policy_arn = aws_iam_policy.report_worker_policy.arn
}
resource "aws_iam_role_policy_attachment" "process_report_lambda_dynamo_access" {
  role       = module.ProcessReportLambda.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}
resource "aws_lambda_permission" "allow_api_gateway_start_report" {
  statement_id = "AllowAPIGatewayInvokeStartReport"
  action        = "lambda:InvokeFunction"
  function_name = module.StartReportLambda.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn = "${aws_api_gateway_rest_api.task_api.execution_arn}/*/${aws_api_gateway_method.post_report_method.http_method}${aws_api_gateway_resource.reports_resource.path}"
}

resource "aws_iam_policy" "sqs_send_policy" {
  name = "lambda-sqs-send-policy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = "sqs:SendMessage",
        Resource = aws_sqs_queue.report_queue.arn
      }
    ]
  })
}
resource "aws_iam_policy" "report_worker_policy" {
  name = "lambda-report-worker-policy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = aws_sqs_queue.report_queue.arn
      },
      {
        Effect = "Allow",
        Action = "s3:PutObject",
        Resource = "${aws_s3_bucket.csv_bucket.arn}/*"
      },
      {
        Effect = "Allow",
        Action = "ses:SendEmail",
        Resource = "*"
      }
    ]
  })
}


resource "aws_sqs_queue" "report_dlq" {
  name = "report-generation-dlq"
  tags = {
    Project = "TODOLambdaJava"
  }
}
resource "aws_sqs_queue" "report_queue" {
  name = "report-generation-queue"
  visibility_timeout_seconds = 300
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.report_dlq.arn
    maxReceiveCount = 3
  })
  tags = {
    Project = "TODOLambdaJava"
  }
}
resource "aws_s3_bucket" "csv_bucket" {
  bucket = "todo-lambda-csv-reports-filipe"
  tags = {
    Project = "TODOLambdaJava"
  }
}
resource "aws_ses_email_identity" "sender_email" {
  email = "limafilipe.coding@gmail.com"
}


output "nome_da_tabela" {
  value = module.todo_table.table_name
}

# --- ARN Lambdas TaskList ---
output "arn_da_create_list_lambda" {
  description = "O ARN da função Lambda de criação da lista tarefas"
  value = module.CreateTaskListLambda.lambda_function_arn
}
output "arn_da_list_taskList_lambda" {
  description = "O ARN da função Lambda da listagem das listas de tarefas"
  value = module.ListTaskListsLambda.lambda_function_arn
}
output "arn_da_get_list_by_id_lambda" {
  description = "O ARN da função Lambda da listagem das listas de tarefas por id"
  value = module.GetTaskListByIdLambda.lambda_function_arn
}
output "arn_delete_list_lambda" {
  description = "O ARN da função Lambda para deletar lista de tarefas"
  value = module.DeleteTaskListLambda.lambda_function_arn
}


# --- ARN Lambdas Task ---
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

