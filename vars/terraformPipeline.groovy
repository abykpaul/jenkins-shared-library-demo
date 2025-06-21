def call(env) {
    dir("infra/${env}") {
        sh "terraform init"
        sh "terraform plan -var-file=${env}.tfvars"
        sh "terraform apply -auto-approve -var-file=${env}.tfvars"
    }
}