def call(Map config) {
  node {
    stage('Init Vars') {
      env.REPO_NAME  = "${config.repoName}"
      env.IMAGE_NAME = "${config.imageName}"
      env.GIT_URL    = "${config.gitUrl}"
      env.AWS_CREDS  = "${config.awsCredsId}"
      env.AWS_REGION = "${config.region}"
    }

    stage('Checkout App') {
      git branch: 'main', credentialsId: '45b1557e-3c87-4706-ac5f-f39c117d15d8', url: env.GIT_URL
    }

    stage('Docker Build') {
      dir("docker_ecr_app_deploy/myapp") {
        bat "docker build -t %REPO_NAME%:latest ."
      }
    }

    stage('Login, Tag & Push to ECR') {
      withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: env.AWS_CREDS]]) {
        dir("docker_ecr_app_deploy/myapp") {
          bat """
            FOR /F %%i IN ('aws sts get-caller-identity --query "Account" --output text') DO (
              CALL SET ACCOUNT_ID=%%i
              CALL aws ecr get-login-password --region %AWS_REGION% ^| docker login --username AWS --password-stdin %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com
              CALL docker tag %REPO_NAME%:latest %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
              CALL docker push %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
            )
          """
        }
      }
    }
  }
}
