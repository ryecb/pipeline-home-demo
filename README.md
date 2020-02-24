# My Pipeline Home

This repo aims to model Post in the CloudBees Blog: [Template Catalog: Welcome to the Pipeline as Code family](https://www.cloudbees.com/blog)

This repor was forked from [beedemo/workflowLibs](https://github.com/beedemo/workflowLibs) then own content was develop from the inspiration of:

* [Pipeline Template Guide](https://docs.cloudbees.com/docs/admin-resources/latest/pipeline-templates-user-guide/) from the CloudBees Docs.
* [Using Kaniko with CloudBees Core guide](https://docs.cloudbees.com/docs/cloudbees-core/latest/cloud-admin-guide/using-kaniko) from the CloudBees Docs
* The post [Share a standard Pipeline across multiple projects with Shared Libraries](https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/) from the Jenkins Blog.
* Jenkins Kubernetes plugins [examples](https://github.com/jenkinsci/kubernetes-plugin/tree/master/examples) for good practices.
* The [simple-app](https://github.com/alecharp/simple-app) project from my colleague Adrien Lecharpentier

## Shared Library

## Global Steps

### mavenProject

Provides a template for maven builds. Additionally, it provides automated creation/updates of customized build images using `docker commit` to include caching all maven dependencies inside of the repo specific custom build image; dramatically speeding up build times.

#### configuration

* `mavenProject`: provides simple config as Pipeline for maven based projects
  * org: GitHub organization or user repo is under
  * repo: GitHub repository being built
  * hipChatRoom: id or name of HipChat room to send build messages
  * jdk: version of JDK to use for build as string value
  * maven: version of Maven to use for build as string value
  * rebuildBuildImage: boolean that controls whether or not to refresh existing repo specific Docker build image based on the `maven' image

#### Example

```groovy
mavenProject {
	org = 'sa-team'
	repo = 'todo-api'
	hipChatRoom = '1613593'
	jdk = '8'
	maven = '3.3.3'
	rebuildBuildImage = true
	protectedBranches = ['master']
}
```