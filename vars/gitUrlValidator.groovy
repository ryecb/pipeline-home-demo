// vars/mavenK8Project.groovy
def call(url) {
  if (! url.startsWith("https") || ! url.endsWith (".git")){
    error("The git url must strat with 'https' and ends with '.git'. Example: https://github.com/example-org/example-repo.git")
  }
}
