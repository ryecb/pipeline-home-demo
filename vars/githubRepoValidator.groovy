// vars/githubRepoValidator.groovy
def call(repo, org, apiUri) {
  if ( repo.trim().startsWith("https") || repo.trim().endsWith (".git")){
    error("A git endpoint is not expected. Enter the name of a repo under ${org} organization} in ${apiUri}")
  }
}
