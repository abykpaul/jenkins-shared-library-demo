def call(Map config) {
    def env     = config.env
    def action  = config.action ?: 'apply'
    def tfvars  = config.tfvars ?: "${env}.tfvars"
    def tfDir   = config.dir ?: "aws-free-tier-project/terraform/${env}"
    def region  = config.region ?: 'ap-south-1'
    def credId  = config.credId

    withCredentials([[ $class: 'AmazonWebServicesCredentialsBinding', credentialsId: credId ]]) {
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

                // 🧪 Health Check after Apply
                echo "🌐 Fetching EC2 Public IP for ${env}"
                def pubIp = bat(
                    script: "terraform -chdir=${tfDir} output -raw public_ip",
                    returnStdout: true
                ).trim()

                echo "🌍 Public IP: ${pubIp}"
                echo "🧪 Performing curl test: http://${pubIp}"

                def result = bat(
                    script: "curl -s -o /dev/null -w \"%{http_code}\" http://${pubIp}",
                    returnStdout: true
                ).trim()

                if (result == "200") {
                    echo "✅ App is running fine at http://${pubIp} (Status: ${result})"
                } else {
                    error "❌ App unreachable at http://${pubIp} (Status: ${result})"
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
