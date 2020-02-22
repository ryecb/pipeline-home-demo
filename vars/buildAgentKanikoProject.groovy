// vars/mavenProject.groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    //used for analytics indexing
    def K8_SECRET = config.k8_secret

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            PATH = "/busybox:/kaniko:$PATH"
            DOCKER_DESTINATION = "${config.docker_registry}/${config.docker_image}:${config.docker_tag}"
            TEAM_NAME = "${config.team_name}"
            TEAM_MAIL = "${config.team_mail}"
            AGENT_TOOLS = "${config.agent_tools}"
        }
        stages {
            stage('Build with Kaniko') {
            agent {
                kubernetes {
                label 'example-kaniko-volumes'
                yaml """
        kind: Pod
        metadata:
        name: kaniko
        spec:
        containers:
        - name: jnlp
            workingDir: /home/jenkins/agent
        - name: kaniko
            workingDir: /home/jenkins/agent
            image: gcr.io/kaniko-project/executor:debug
            imagePullPolicy: Always
            command:
            - /busybox/cat
            tty: true
            volumeMounts:
            - name: jenkins-docker-cfg
                mountPath: /kaniko/.docker
        volumes:
        - name: jenkins-docker-cfg
            projected:
            sources:
            - secret:
                name:  ${K8_SECRET}
                items:
                    - key: .dockerconfigjson
                    path: config.json
        """
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
