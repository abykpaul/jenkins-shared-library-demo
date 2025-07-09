def call(Map config) {
    def env     = config.env
    def action  = config.action ?: 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {
        withEnv(["AWS_REGION=ap-south-1"]) {
            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            if (action == 'apply') {
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

            } else if (action == 'destroy') {
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                timeout(time: 2, unit: 'MINUTES') {
                    input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                }

                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"

            } else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
