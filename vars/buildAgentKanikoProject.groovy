// vars/mavenProject.groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    K8_AGENT_YAML = "${config.k8_agent_yaml}"

    pipeline {
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            PATH = "/busybox:/kaniko:$PATH"
            GITHUB_CREDENTIALS = "${config.gh_cred}"
            GITHUB_REPO = "${config.gh_repo}"
            DOCKERFILE_REPO = "${config.dockerfile_repo}"
            DOCKER_DESTINATION = "${config.docker_registry}/${config.docker_image}:${config.docker_tag}"
        }
        agent {
            kubernetes {
                defaultContainer "maven"
                yaml libraryResource("${K8_AGENT_YAML}")
            }
        }
        stages {
            stage('Checkout app') {
                steps {
                    git branch: 'develop', credentialsId: "${config.gh_cred}" , url: "${config.gh_repo}"
                }
            }
            stage('Build app') {
                steps {
                    sh "mvn clean package -Dmaven.test.skip=true"
                    archiveArtifacts artifacts: "target/*.jar", fingerprint: true
                    stash name: "docker", includes: "${DOCKERFILE_REPO}, target/*.jar", excludes: '.git'
                }
            }
            stage('Build and Publish Image app') {
                steps {
                    container(name: 'kaniko', shell: '/busybox/sh') {
                        unstash 'docker'
                        sh """#!/busybox/sh
                            /kaniko/executor --context `pwd` --verbosity debug --destination ${DOCKER_DESTINATION}
                        """
                    }
                }
            }
        }
        // post {
        //     failure {
        //         mail to: "${TEAM_MAIL}",
        //             subject: "Failed Pipeline to Build Agent: ${currentBuild.fullDisplayName}",
        //             body: "Something is wrong with ${env.BUILD_URL}"
        //     }
        // }
    }
}
