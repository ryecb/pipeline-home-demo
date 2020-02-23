// vars/mavenProject.groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            PATH = "/busybox:/kaniko:$PATH"
            K8_AGENT_YAML = "${config.k8_agent_yaml}"
            DOCKER_DESTINATION = "${config.docker_registry}/${config.docker_image}:${config.docker_tag}"
            TEAM_NAME = "${config.team_name}"
            TEAM_MAIL = "${config.team_mail}"
            AGENT_TOOLS = "${config.agent_tools}"
        }
        stages {
            stage('Build with Kaniko') {
            agent {
                kubernetes {
                    yaml libraryResource("${K8_AGENT_YAML}")
                }
            }
            steps {
                container(name: 'kaniko', shell: '/busybox/sh') {

                writeFile file: "Dockerfile", text: """
                FROM jenkins/slave:4.0-1
                MAINTAINER ${TEAM_NAME} <${TEAM_MAIL}>
                USER root
                RUN rm -rf /var/lib/apt/lists/*
                RUN ${AGENT_TOOLS}
                USER jenkins
                """

                sh """#!/busybox/sh
                    /kaniko/executor --context `pwd` --verbosity debug --destination ${DOCKER_DESTINATION}
                """
                }
            }
            }
        }
        post {
            failure {
                mail to: "${TEAM_MAIL}",
                    subject: "Failed Pipeline to Build Agent: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
            }
        }
    }
}
