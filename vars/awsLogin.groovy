def call() {
    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-ecr-creds' ]]) {
        bat 'set AWS_REGION=ap-south-1'
        bat 'aws sts get-caller-identity'
        bat 'echo ✅ AWS credentials set for Terraform.'
    }
}
