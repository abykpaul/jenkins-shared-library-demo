// 👇 Shared Library function: Automates Terraform workflow (init, validate, apply/destroy, health check)
def call(Map config) {
    def env     = config.env                          // 👉 'dev' or 'prod'
    def action  = config.action ?: 'apply'            // 👉 Default action is 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"    // 👉 tfvars file per environment
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}" // 👉 Terraform working directory

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {
        withEnv(["AWS_REGION=ap-south-1"]) {

            // 🔧 Terraform Init & Validate
            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            if (action == 'apply') {
                // 📋 Terraform Plan
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                // ✅ Manual approval for ALL environments (dev + prod)
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                // 🚀 Terraform Apply
                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🌍 Output public IP from Terraform
                def publicIp = bat(script: "terraform -chdir=${tfDir} output -raw public_ip", returnStdout: true).trim()
                echo "🧪 Performing health check on: http://${publicIp}"

                // 💡 Use PowerShell for HTTP status code check on Windows agents
                def statusCode = powershell(
                    script: """
                        \$response = Invoke-WebRequest -Uri "http://${publicIp}" -UseBasicParsing -TimeoutSec 10
                        Write-Output \$response.StatusCode
                    """,
                    returnStdout: true
                ).trim()

                // 🔍 Validate status
                if (statusCode != '200') {
                    error "❌ Health check failed. Expected 200, got ${statusCode}"
                } else {
                    echo "✅ Health check passed: HTTP ${statusCode}"
                }

            } else if (action == 'destroy') {
                // 💣 Terraform Destroy Plan
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                // ⚠️ Ask confirmation before destruction (for ALL environments)
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                }

                // 🧨 Destroy resources
                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"

            } else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
