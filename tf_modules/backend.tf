terraform {
  backend "s3" {
    bucket = "terraform-state-todo-lambda-filipe-20250926"
    key = "global/s3/terraform.tfstate"
    region = "sa-east-1"

    dynamodb_table = "terraform-state-lock"
    encrypt = true

    profile = "tf_windows_filipelima"
  }
}
