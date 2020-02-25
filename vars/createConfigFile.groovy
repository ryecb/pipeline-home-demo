// vars/createConfigFile.groovy
def call(yaml) {
   def pipelineConfig = new File('pipelineConfig.yaml')
   pipelineConfig << "${yaml}" 
}
