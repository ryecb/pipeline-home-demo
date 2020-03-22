# Pipeline Home Demo

This repo is linked to the post `Template Catalog: Welcome to the Pipeline as Code family` from the [CloudBees Blog](https://www.cloudbees.com/blog).

<div style="margin: auto; border: padding: 10px; text-align: center;">
    <img alt="" src="resources/img/fusion.png" style="width: 300px; ;" />
</div>

Not only the [Template Catalog](https://docs.cloudbees.com/docs/admin-resources/latest/pipeline-templates-user-guide/setting-up-a-pipeline-template-catalog) and [Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) are complementary to each other but also compatible in their code structures. That’s why they can be integrated into a single point of control which it was named in the post as the "The Pipeline Home".

## Demo: Maven Docker App run by Kubernetes Cloud Agents

This template model the pipeline Continous Integration (CI) process for Maven Apps than are deployed as Docker containers. It integrates CloudBees Core and GitHub Enterprise via webhooks. Builds are managed by Kubernetes agents, successful ones upload images to Dockerhub.