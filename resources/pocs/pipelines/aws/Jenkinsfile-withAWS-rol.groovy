// resources/pocs/pipelines/aws/Jenkinsfile-withAWS.groovy
pipeline {
    agent {
        kubernetes {
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
                withAWS(profile:'cloudbees-support-admin') { // Pre-requisite: it must be defined in '~/.aws/config'
                    echo "Trying 'aws sts get-caller-identity'"
                    sh "aws sts get-caller-identity"
                    echo "Trying 'awsIdentity()'"
                    def identity = awsIdentity()
                }
            }
        }
    }
}