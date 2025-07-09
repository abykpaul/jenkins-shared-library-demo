// 👉 Shared Library function: Automates Terraform apply/destroy with health check + user input confirmation

def call(Map config) {
    // 🧾 Incoming parameters
    def env     = config.env                          // Environment name: dev/prod
    def action  = config.action ?: 'apply'            // Action type: apply or destroy
    def tfvars  = config.tfvars ?: "${env}.tfvars"    // tfvars file path
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"  // Terraform directory

    // 🔐 Load AWS credentials securely from Jenkins Credentials
    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: config.credId ]]) {

        // 🌍 Set AWS Region environment variable
        withEnv(["AWS_REGION=ap-south-1"]) {

            // 🔧 Terraform Init & Validate
            bat "terraform -chdir=${tfDir} init -reconfigure"   // Reinitialize Terraform backend
            bat "terraform -chdir=${tfDir} validate"            // Validate Terraform code

            if (action == 'apply') {
                // 📋 Terraform Plan
                bat "terraform -chdir=${tfDir} plan -var-file=${tfvars}"

                // 🔐 Wait for manual approval (even for dev)
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "🟢 Approve Terraform APPLY for ${env}?", ok: "Yes, Apply"
                }

                // 🚀 Terraform Apply
                bat "terraform -chdir=${tfDir} apply -auto-approve -var-file=${tfvars}"
                echo "✅ Apply completed for ${env}"

                // 🌐 Fetch public IP from Terraform output
                def publicIp = bat(script: "terraform -chdir=${tfDir} output -raw public_ip", returnStdout: true).trim()

                echo "🧪 Performing health check on: http://${publicIp}"

                // 🔍 Run curl test (HTTP health check)
                def responseCode = powershell(returnStdout: true, script: """
                    try {
                        \$response = Invoke-WebRequest -Uri 'http://${publicIp}' -UseBasicParsing -TimeoutSec 10
                        return \$response.StatusCode
                    } catch {
                        return 0
                    }
                """).trim()

                // ❌ Fail if HTTP response is not 200
                if (responseCode != "200") {
                    error "❌ Health check failed. Expected 200, got ${responseCode}"
                } else {
                    echo "✅ Health check successful for ${env} (${publicIp})"
                }

            } else if (action == 'destroy') {
                // 🔄 Terraform Destroy Plan
                bat "terraform -chdir=${tfDir} plan -destroy -var-file=${tfvars}"

                // 🛑 Wait for user input confirmation to destroy
                timeout(time: 2, unit: 'MINUTES') {
                    input message: "💥 Confirm Terraform DESTROY for ${env}?", ok: "Yes, Destroy"
                }

                // 🔥 Terraform Destroy
                bat "terraform -chdir=${tfDir} destroy -auto-approve -var-file=${tfvars}"
                echo "💥 Destroy completed for ${env}"

            } else {
                // 🚫 Invalid action
                echo "📋 Unsupported action: ${action}"
            }
        }
    }
}
