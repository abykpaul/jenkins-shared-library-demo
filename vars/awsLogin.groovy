def call() {
    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: 'aws-ecr-creds' // ✅ You must add this in Jenkins
    ]]) {
        bat 'aws sts get-caller-identity'
        bat 'echo AWS credentials set for Terraform.'
    }
}
