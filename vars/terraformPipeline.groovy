def call(String env) {
    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: 'aws-ecr-creds'
    ]]) {
        bat "echo Initializing Terraform for ${env}"
        bat "terraform -chdir=infra/${env} init"
        bat "terraform -chdir=infra/${env} apply -var-file=${env}.tfvars -auto-approve"
    }
}
