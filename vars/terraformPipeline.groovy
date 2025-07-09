def call(Map config) {
    def env     = config.env
    def action  = config.action ?: 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {
        withEnv(["AWS_REGION=ap-south-1"]) {

            // 🛠️ Terraform init & validate
            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            if (action == 'apply') {

                // 📋 Plan
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                // 🟢 Approval prompt with timeout
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                // ✅ Apply
                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🧪 Health check
                def publicIp = powershell(
                    script: "terraform -chdir=${tfDir} output -raw public_ip",
                    returnStdout: true
                ).trim()

                echo "🧪 Performing curl test: http://${publicIp}"

                def response = powershell(
                    script: "curl -s -o \$null -w \"%{http_code}\" http://${publicIp}",
                    returnStdout: true
                ).trim()

                if (response != "200") {
                    error "❌ Health check failed. Expected 200, got ${response}"
                } else {
                    echo "✅ Health check passed with HTTP ${response}"
                }

            } else if (action == 'destroy') {

                // 🧨 Plan destroy
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                // ⚠️ Destroy confirmation with timeout
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                }

                // 💥 Destroy
                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"

            } else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
