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

resource "aws_iam_policy" "lambda_dynamodb_policy" {
  name = "lambda-dynamodb-tasks-policy"
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
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
          "dynamodb:Query"
        ],
        "Resource": module.todo_table.table_arn
      }
    ]
  })
}


# ----- Módulo Lambda ------

module "minha_lambda_hello_world" {
  source = "../tf_modules/lambda"

  function_name     = "hello-world-lambda-java"
  handler           = "dev.filipe.TODOLambdaJava.Handler::handleRequest"
  runtime           = "java21"
  source_code_path  = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size       = 1024
  timeout           = 60
  tags = {
    Project = "TODOLambdaJava"
   ManagedBy   = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "lambda_dynamodb_access" {
  role = module.minha_lambda_hello_world.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_policy.arn
}

output "nome_da_tabela" {
  value = module.todo_table.table_name
}
output "arn_da_lambda" {
  description = "O ARN da função Lambda criada pelo módulo"
  value = module.minha_lambda_hello_world.lambda_function_arn
}

