@Library('my-shared-lib') _

pipeline {
    agent any
    stages {
        stage('CI Pipeline') {
            steps {
                script {
                    ciPipeline('dev')
                }
            }
        }
        stage('Terraform') {
            steps {
                script {
                    terraformPipeline('dev')
                }
            }
        }
        stage('AWS Login') {
            steps {
                script {
                    awsLogin()
                }
            }
        }
        stage('Approval') {
            steps {
                script {
                    manualApproval("Proceed to Production?")
                }
            }
        }
        stage('Retry Example') {
            steps {
                script {
                    withRetry {
                        sh 'echo "Simulated command..."'
                    }
                }
            }
        }
    }
}
