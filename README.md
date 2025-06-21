# Jenkins Shared Library Demo

## ✅ Use Cases Covered
1. Standard CI/CD Steps (`ciPipeline`)
2. Terraform Automation (`terraformPipeline`)
3. Secure AWS Login (`awsLogin`)
4. Manual Approval Step (`manualApproval`)
5. Retry Logic (`withRetry`)

## 📘 Malayalam Explanation (സംഗ്രഹം)
- എല്ലാ repeat ആകുന്ന pipeline functions centralized ആയി reuse ചെയ്യാം.
- ഒറ്റ Jenkinsfile-ൽ നിന്നും shared-lib folder-ൽ function വിളിക്കാൻ കഴിയും.
- `@Library('my-shared-lib') _` → എന്നതിനാൽ shared functions എല്ലാ stage-ലും ഉപയോഗിക്കാം.

## 🔧 Setup in Jenkins
- Manage Jenkins → Configure System → Global Pipeline Libraries → Add
- Name: `my-shared-lib`
- Source: Git, point to this repo

Happy Automating! 🚀