// File: vars/ecrPush.groovy

def call(Map config = [:]) {
  pipeline {
    agent any

    environment {
      AWS_REGION = config.region ?: 'ap-south-1'
      IMAGE_NAME = config.imageName ?: 'myapp-image'
      ECR_REPO = config.repoName ?: 'my-ecr-repo'
    }

    stages {
      stage('Checkout Code') {
        steps {
          git url: config.gitUrl ?: 'https://github.com/abykpaul/vault_jenkins_int.git'
        }
      }

      stage('Build Docker Image') {
        steps {
          script {
            dockerImage = docker.build(IMAGE_NAME)
          }
        }
      }

      stage('Login to AWS ECR') {
        steps {
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.awsCredsId ?: 'aws-ecr-creds']]) {
            sh "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
          }
        }
      }

      stage('Tag & Push Image to ECR') {
        steps {
          script {
            ACCOUNT_ID = sh(script: "aws sts get-caller-identity --query Account --output text", returnStdout: true).trim()
            IMAGE_TAG = "$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:latest"
            sh "docker tag $IMAGE_NAME $IMAGE_TAG"
            sh "docker push $IMAGE_TAG"
          }
        }
      }
    }
  }
}
