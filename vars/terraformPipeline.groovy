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

                if (env == "prod") {
                    input message: "⚠️ Approve deployment to PROD?", ok: "Yes, Deploy"
                } else {
                    echo "🧪 Proceeding with DEV deployment"
                }

                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

            } else if (action == 'destroy') {
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                if (env == "prod") {
                    input message: "⚠️ Confirm destruction of PROD?", ok: "Yes, Destroy"
                } else {
                    echo "🧹 Proceeding with DEV destruction"
                }

                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"
            } else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
