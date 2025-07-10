// ✅ Shared library method: Deploy or destroy infra using Terraform
// ✅ This includes input prompt, plan, apply/destroy, and health check with retry logic

def call(Map config) {
    // 🧾 Inputs from Jenkinsfile parameters or defaults
    def env     = config.env                       // environment like dev/prod
    def action  = config.action ?: 'apply'         // apply or destroy
    def tfvars  = config.tfvars ?: "${env}.tfvars" // tfvars filename
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"
    def credId  = config.credId                    // AWS credentials ID from Jenkins

    // 🔐 AWS Credentials binding from Jenkins Credentials
    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: credId ]]) {

        // 🌐 AWS_REGION setup
        withEnv(["AWS_REGION=ap-south-1"]) {

            // 📦 Terraform init & validate
            bat "terraform -chdir=${tfDir} init -reconfigure"
            bat "terraform -chdir=${tfDir} validate"

            // 📌 Action: APPLY
            if (action == 'apply') {
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                // ✅ Apply confirmation for both dev & prod (with timeout)
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🌍 Health check after deploy
                try {
                    // 👉 Capture only the IP using last line of output
                    def rawOutput = bat(script: "terraform -chdir=${tfDir} output -raw public_ip", returnStdout: true).trim()
                    def publicIp = rawOutput.readLines().last().trim()

                    echo "🧪 Performing health check on: http://${publicIp}"

                    def attempts = 0
                    def maxAttempts = 5
                    def statusCode = ''

                    while (attempts < maxAttempts) {
                        statusCode = powershell(
                            script: """
                                try {
                                    \$response = Invoke-WebRequest -Uri "http://${publicIp}" -UseBasicParsing -TimeoutSec 5
                                    Write-Output \$response.StatusCode
                                } catch {
                                    Write-Output 0
                                }
                            """,
                            returnStdout: true
                        ).trim()

                        if (statusCode == '200') {
                            echo "✅ Health check passed on attempt ${attempts + 1}: HTTP ${statusCode}"
                            break
                        } else {
                            echo "⏳ Attempt ${attempts + 1}/5 failed. Status: ${statusCode}. Retrying in 5 seconds..."
                            sleep(time: 5, unit: 'SECONDS')
                            attempts++
                        }
                    }

                    if (statusCode != '200') {
                        error "❌ Health check failed after ${maxAttempts} attempts. Last status: ${statusCode}"
                    }

                } catch (err) {
                    error "❌ Health check script failed: ${err.message}"
                }
            }

            // 🔥 Action: DESTROY
            else if (action == 'destroy') {
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                // 🧨 Destroy confirmation
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                }

                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"
            }

            // ❓ Invalid action
            else {
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
