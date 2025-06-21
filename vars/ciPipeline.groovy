def call(Map config) {
    stage('Checkout') {
        checkout scm
    }
    stage('Build') {
        echo "Building application for environment: ${config.env}"
        sh 'echo "npm install"'
    }
    stage('Test') {
        sh 'echo "Running tests..."'
    }
    stage('Deploy') {
        sh "echo Deploying to ${config.env} environment"
    }
}