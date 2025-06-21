@Library('my-shared-lib') _

pipeline {
    agent any

    environment {
        SHELL = 'C:\\\\Program Files\\\\Git\\\\bin\\\\bash.exe'
    }

    stages {
        stage('AWS Login') {
            steps {
                script {
                    awsLogin() // ✅ AWS credentials injected first
                }
            }
        }

        stage('Terraform') {
            steps {
                script {
                    terraformPipeline('dev') // ✅ Terraform runs after login
                }
            }
        }

        // other stages...
    }
}
