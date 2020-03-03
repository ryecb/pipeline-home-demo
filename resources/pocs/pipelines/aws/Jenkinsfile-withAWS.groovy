// It requires 'support_vault_aws'
pipeline {
    agent {
        kubernetes {
          //cloud 'kubernetes'
          containerTemplate {
            name 'aws-cli'
            image 'carlosrodlop/jenkins-slave-aws:e7c3999'
            ttyEnabled true
            }
        }
    }
    stages {
        stage('hello AWS') {
            steps {
                withAWS(credentials: 'support_vault_aws') {
                    sh "aws sts get-caller-identity"
                }
            }
        }
    }
}