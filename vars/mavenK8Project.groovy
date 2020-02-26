// vars/mavenK8Project.groovy
def call(configYaml) {

    Map config = readYaml text: "${configYaml}"

    K8_AGENT_YAML = "${config.k8_agent_yaml}" //It does not work if it is moved to the environment section
    GITHUB_BRANCH = "${config.gh_branch}"
    PROTECTED_BRANCH = "master"
    git_short_commit = ""
    git_currentBranch = ""
    git_repo = ""

    pipeline {
        agent {
            kubernetes {
                defaultContainer "git-maven"
                yaml libraryResource("agents/k8s/${K8_AGENT_YAML}")
            }
        }
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            DOCKERFILE_PATH = "${config.dockerfile_path}"
        }
        stages {
            // branch only works on a multibranch Pipeline.
            stage ("Skip CD/CI for protected branch") {
                when {
                    branch "${PROTECTED_BRANCH}"
                }
                steps {
                    error("Invalid target branch: master")
                }
            }
            stage ("Run") {
                stages {
                    stage("Print configuration") {
                        steps {
                            writeYaml file: "config.yaml", data: config  
                            sh "cat config.yaml"
                        }
                    }
                    stage("Checkout") {
                        environment {
                            GITHUB_CREDENTIALS = "${config.gh_cred}"
                            GITHUB_REPO = "${config.gh_repo}"
                        }
                        steps {
                            container(name: "git-maven"){
                                script {
                                    if (GITHUB_BRANCH == "") {
                                        echo "Pipeline Multibranch detected"
                                        git credentialsId: "${GITHUB_CREDENTIALS}" , url: "${GITHUB_REPO}"
                                    } else {
                                        echo "Pipeline non Multibranch detected"
                                        git branch: "${GITHUB_BRANCH}", credentialsId: "${GITHUB_CREDENTIALS}" , url: "${GITHUB_REPO}"
                                    }
                                    git_short_commit = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                                    git_currentBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                                    git_repo = sh(script: "echo `basename '$GITHUB_REPO' ''.${GITHUB_REPO##*.}'`", returnStdout: true).trim()
                                }
                            }
                        }
                    }
                    stage("Build app") {
                        steps {
                            sh "mvn clean package -Dmaven.test.skip=true"
                            archiveArtifacts artifacts: "config.yaml, target/*.jar", fingerprint: true
                            stash name: "docker", includes: "config.yaml, target/*.jar, ${DOCKERFILE_PATH}"
                        }
                    }
                    stage("Build and Publish Image app") {
                       environment {
                          DOCKER_DESTINATION = "${config.docker_registry}/${git_repo}_${git_currentBranch}:${git_short_commit}"
                       }
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
