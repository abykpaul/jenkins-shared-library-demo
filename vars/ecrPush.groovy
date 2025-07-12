def call(Map config) {
  pipeline {
    agent any

    environment {
      AWS_REGION = config.region
      IMAGE_NAME = config.imageName
      REPO_NAME  = config.repoName
    }

    stages {
      stage('Checkout App') {
        steps {
          git url: config.gitUrl
        }
      }

      stage('Docker Build') {
        steps {
          script {
            bat "docker build -t %REPO_NAME%:latest ."
          }
        }
      }

      stage('Login to ECR') {
        steps {
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.awsCredsId]]) {
            script {
              def accountId = bat(
                script: 'aws sts get-caller-identity --query "Account" --output text',
                returnStdout: true
              ).trim()

              def repoUri = "${accountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

              bat """
                for /f %%i in ('aws sts get-caller-identity --query "Account" --output text') do set ACCOUNT_ID=%%i
                aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com
              """
            }
          }
        }
      }

      stage('Tag & Push Image') {
        steps {
          script {
            bat """
              for /f %%i in ('aws sts get-caller-identity --query "Account" --output text') do set ACCOUNT_ID=%%i
              docker tag %REPO_NAME%:latest %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
              docker push %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
            """
          }
        }
      }
    }
  }
}
