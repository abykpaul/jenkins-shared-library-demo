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
      git url: env.GIT_URL
    }

    stage('Docker Build') {
      bat "docker build -t %REPO_NAME%:latest ."
    }

    stage('Login to ECR') {
      withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: env.AWS_CREDS]]) {
        bat """
          FOR /F %%i IN ('aws sts get-caller-identity --query "Account" --output text') DO SET ACCOUNT_ID=%%i
          aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com
        """
      }
    }

    stage('Tag & Push Image') {
      bat """
        FOR /F %%i IN ('aws sts get-caller-identity --query "Account" --output text') DO SET ACCOUNT_ID=%%i
        docker tag %REPO_NAME%:latest %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
        docker push %%ACCOUNT_ID%%.dkr.ecr.%AWS_REGION%.amazonaws.com/%REPO_NAME%:latest
      """
    }
  }
}
