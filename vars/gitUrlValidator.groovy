// vars/gitUrlValidator.groovy
def call(url) {
  if (! url.trim().startsWith("https") || ! url.trim().endsWith (".git")){
    error("The git url must start with 'https' and ends with '.git'. Example: https://github.com/example-org/example-repo.git")
  }
}
