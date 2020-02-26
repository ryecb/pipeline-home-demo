// vars/mavenK8Project.groovy
def call(configYaml) {

    Map config = readYaml text: "${configYaml}"

    K8_AGENT_YAML = "${config.k8_agent_yaml}" //It does not work if it is moved to the environment section
    GITHUB_BRANCH = "${config.gh_branch}"

    pipeline {
        agent none
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            GITHUB_CREDENTIALS = "${config.gh_cred}"
            GITHUB_REPO = "${config.gh_repo}"
            DOCKERFILE_PATH = "${config.dockerfile_path}"
            DOCKER_DESTINATION = "${config.docker_registry}/${config.docker_image}:${config.docker_tag}"
        }
        stages {
            stage ("Validation adn Checkout") {
                stages {
                    stage("Print configuration") {
                        steps {
                            writeYaml file: "config.yaml", data: config  
                            sh "cat config.yaml"
                        }
                    }
                    stage("Checkout app") {
                        when {
                            expression {
                                GIT_BRANCH = 'origin/' + sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                                return ! GIT_BRANCH == 'origin/master'
                            }
                        }
                        steps {
                            script {
                                if (GITHUB_BRANCH == "") {
                                    echo "Pipeline Multibranch detected"
                                    git credentialsId: "${GITHUB_CREDENTIALS}" , url: "${GITHUB_REPO}"
                                } else {
                                    echo "Pipeline non Multibranch detected"
                                    git branch: "${GITHUB_BRANCH}", credentialsId: "${GITHUB_CREDENTIALS}" , url: "${GITHUB_REPO}"
                                }
                            }
                        }
                    }
                    stage ('Build Skipped') {
                        when {
                            expression {
                                GIT_BRANCH = 'origin/' + sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                                return GIT_BRANCH == 'origin/master'
                            }
                        }
                        steps {
                            error("Invalid target branch: master")
                        }
                    }
                }
            }
            stage ("Build and Publish") {
                agent {
                    kubernetes {
                        defaultContainer "maven"
                        yaml libraryResource("agents/k8s/${K8_AGENT_YAML}")
                    }
                }
                stages {
                    stage("Build app") {
                        steps {
                            sh "mvn clean package -Dmaven.test.skip=true"
                            archiveArtifacts artifacts: "config.yaml, target/*.jar", fingerprint: true
                            stash name: "docker", includes: "config.yaml, target/*.jar, ${DOCKERFILE_PATH}"
                        }
                    }
                    stage("Build and Publish Image app") {
                        steps {
                            container(name: "kaniko", shell: "/busybox/sh") {
                                dir("to_build") { 
                                    unstash "docker"
                                    sh "/kaniko/executor --dockerfile `pwd`/${DOCKERFILE_PATH} --context `pwd` --destination ${DOCKER_DESTINATION}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
