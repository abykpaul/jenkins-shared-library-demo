def call(Map config) {
    def env     = config.env
    def action  = config.action ?: 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {
        withEnv(["AWS_REGION=ap-south-1"]) {

            // Init and Validate
            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            if (action == 'apply') {
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                // ✅ Optional Approval
                if (env == 'prod') {
                    timeout(time: 2, unit: 'MINUTES') {
                        input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                    }
                }

                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🧪 Health Check
                def publicIp = powershell(
                    script: "terraform -chdir=${tfDir} output -raw public_ip",
                    returnStdout: true
                ).trim()

                echo "🧪 Performing health check on: http://${publicIp}"

                def response = powershell(
                    script: """
                    try {
                        \$resp = Invoke-WebRequest -Uri "http://${publicIp}" -UseBasicParsing -TimeoutSec 5
                        \$resp.StatusCode
                    } catch {
                        Write-Output 0
                    }
                    """,
                    returnStdout: true
                ).trim()

                if (response != "200") {
                    error "❌ Health check failed. Expected 200, got ${response}"
                } else {
                    echo "✅ Health check passed with HTTP ${response}"
                }

            } else if (action == 'destroy') {
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                if (env == 'prod') {
                    timeout(time: 2, unit: 'MINUTES') {
                        input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                    }
                }

                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"

            } else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
