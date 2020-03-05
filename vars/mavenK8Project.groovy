// vars/mavenK8Project.groovy
def call(configYaml) {

    Map config = readYaml text: "${configYaml}"

    K8_AGENT_YAML = "${config.k8_agent_yaml}"
    GITHUB_BRANCH = "${config.gh_branch}"
    git_commit = ""
    git_currentBranch = ""
    git_repo = ""

    pipeline {
        agent {
            kubernetes {
                defaultContainer "git-maven"
                yaml libraryResource("agents/k8s/java/${K8_AGENT_YAML}")
            }
        }
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            DOCKERFILE_PATH = "${config.dockerfile_path}"
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
                            // See https://github.com/jenkinsci/git-plugin#environment-variables
                            git_commit = "${GIT_COMMIT}"
                            git_currentBranch = "${GIT_BRANCH}"
                            git_repo = sh(script: "basename '${GITHUB_REPO}' .git", returnStdout: true).trim()
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
                    DOCKER_DESTINATION = "${config.docker_registry}/${git_repo}_${git_currentBranch}:${git_commit}"
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
