// vars/mavenK8Project.groovy
def call(configYaml) {

    Map config = readYaml text: "${configYaml}"

    K8_AGENT_YAML = "${config.k8_agent_yaml}" //It does not work if it is moved to the environment section

    pipeline {
        options {
            buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        }
        environment {
            GITHUB_CREDENTIALS = "${config.gh_cred}"
            GITHUB_REPO = "${config.gh_repo}"
            GITHUB_BRANCH = "${config.gh_branch}"
            DOCKERFILE_PATH = "${config.dockerfile_path}"
            DOCKER_DESTINATION = "${config.docker_registry}/${config.docker_image}:${config.docker_tag}"
        }
        agent {
            kubernetes {
                defaultContainer "maven"
                yaml libraryResource("agents/k8s/${K8_AGENT_YAML}")
            }
        }
        stages {
            stage("Print configuration") {
                steps {
                   writeYaml file: "config.yaml", data: config  
                   sh "cat config.yaml"
                }
            }
            stage("Checkout app") {
                steps {
                    script {
                        if (GITHUB_BRANCH == "") {
                            echo "Pipeline Multibranch detected"
                            checkout([$class: "GitSCM", 
                                branches: [[name: ":^(?!origin/master$).*"]],
                                browser: [$class: 'GitWeb', repoUrl: "https://github.com/carlosrodlop/simple-app"], 
                                userRemoteConfigs: [[credentialsId: "${GITHUB_CREDENTIALS}", url: "${GITHUB_REPO}"]]])
                        } else {
                            echo "Pipeline non Multibranch detected"
                            git branch: "${GITHUB_BRANCH}", credentialsId: "${GITHUB_CREDENTIALS}" , url: "${GITHUB_REPO}"
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
