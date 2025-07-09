def call(Map config) {
    def env     = config.env
    def action  = config.action ?: 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"
    def region  = config.region ?: 'ap-south-1'

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {
        withEnv(["AWS_REGION=${region}"]) {

            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            if (action == 'apply') {
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🧪 Health Check (curl)
                def publicIp = bat(script: "terraform -chdir=${tfDir} output -raw public_ip", returnStdout: true).trim()
                echo "🧪 Performing curl test: http://${publicIp}"

                def response = bat(
                    script: """curl -s -o nul -w "%{http_code}" http://${publicIp}""",
                    returnStdout: true
                ).trim()

                if (response != "200") {
                    error "❌ Health check failed. Expected 200, but got ${response}"
                } else {
                    echo "✅ Health check passed with HTTP ${response}"
                }

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
