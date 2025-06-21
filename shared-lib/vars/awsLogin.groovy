def call() {
    withCredentials([
        usernamePassword(credentialsId: 'aws-ecr-creds',
                         usernameVariable: 'AWS_ACCESS_KEY_ID',
                         passwordVariable: 'AWS_SECRET_ACCESS_KEY')
    ]) {
        sh 'aws ecr get-login-password | docker login --username AWS --password-stdin <your-registry>'
    }
}