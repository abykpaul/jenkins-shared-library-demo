def call(String env) {
    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: 'aws-ecr-creds'
    ]]) {
        bat "echo Destroying Terraform Infra for ${env}"
        bat "terraform -chdir=infra/${env} destroy -var-file=${env}.tfvars -auto-approve"
    }
}
