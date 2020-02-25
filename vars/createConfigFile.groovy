// vars/createConfigFile.groovy
// Requires 
// java.io.File java.lang.String
// staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods leftShift java.io.File java.lang.Object
def call(yaml) {
   def pipelineConfig = new File('pipelineConfig.yaml')
   pipelineConfig << "${yaml}" 
}
