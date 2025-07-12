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
            sh "docker build -t $REPO_NAME:latest ."
          }
        }
      }

      stage('Login to ECR') {
        steps {
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.awsCredsId]]) {
            sh """
              aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin \
              $(aws sts get-caller-identity --query 'Account' --output text).dkr.ecr.$AWS_REGION.amazonaws.com
            """
          }
        }
      }

      stage('Tag & Push Image') {
        steps {
          script {
            def accountId = sh(script: "aws sts get-caller-identity --query 'Account' --output text", returnStdout: true).trim()
            def repoUri = "${accountId}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"

            sh """
              docker tag $REPO_NAME:latest $repoUri:latest
              docker push $repoUri:latest
            """
          }
        }
      }
    }
  }
}
