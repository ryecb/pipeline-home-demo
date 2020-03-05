// vars/mavenK8Project.groovy
def call(configYaml) {

    Map config = readYaml text: "${configYaml}"

    K8_AGENT_YAML = "${config.k8_agent_yaml}"
    GIT_PARAM_BRANCH = "${config.g_branch}"
    git_commit = ""
    git_currentBranch = ""
    git_repo = ""

    pipeline {
        agent {
            kubernetes {
                defaultContainer "git-maven"
                yaml libraryResource("agents/k8s/java/${K8_AGENT_YAML}.yaml")
            }
        }
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            DOCKERFILE_PATH = "${config.d_path}"
        }
        stages {
            stage("Print configuration") {
                steps {
                    writeYaml file: "config.yaml", data: config  
                    sh "cat config.yaml"
                }
            }
            stage("Checkout") {
                environment {
                    GIT_PARAM_CREDENTIALS = "${config.g_cred}"
                    GIT_PARAM_REPO = "${config.g_repo}"
                    DOCKER_IMAGE_LATEST = ${config.d_latest}
                }
                steps {
                    container(name: "git-maven"){
                        script {
                            if (GIT_PARAM_BRANCH?.trim()) {
                                echo "Pipeline Multibranch detected"
                                git credentialsId: "${GIT_PARAM_CREDENTIALS}" , url: "${GIT_PARAM_REPO}"
                            } else {
                                echo "Pipeline non Multibranch detected"
                                git branch: "${GIT_PARAM_BRANCH}", credentialsId: "${GIT_PARAM_CREDENTIALS}" , url: "${GIT_PARAM_REPO}"
                            }
                            // See https://github.com/jenkinsci/git-plugin#environment-variables
                            git_currentBranch = "${GIT_BRANCH}"
                            git_repo = sh(script: "basename '${GIT_PARAM_REPO}' .git", returnStdout: true).trim()
                            echo "DOCKER_IMAGE_LATEST : ${DOCKER_IMAGE_LATEST}" 
                            if (!DOCKER_IMAGE_LATEST) {
                                echo "Tagging image with commit"
                                git_commit = sh(script: "git rev-parse --short=4 ${GIT_COMMIT}", returnStdout: true).trim()
                            } else {
                                echo "Tagging image as latest"
                                git_commit = "latest"
                            } 
                        }
                    }
                }
            }
            stage("Build") {
                steps {
                    sh "mvn clean package -Dmaven.test.skip=true"
                    archiveArtifacts artifacts: "config.yaml, target/*.jar", fingerprint: true
                    stash name: "docker", includes: "config.yaml, target/*.jar, ${DOCKERFILE_PATH}"
                }
            }
            stage("Test") {
                steps {
                    sh 'mvn clean test'
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
            stage("Publish in Registry") {
                environment {
                    DOCKER_DESTINATION = "${config.d_registry}/${git_repo}_${git_currentBranch}:${git_commit}"
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
