def utilRepo, jenkinsGroovy, reuseFunction, gitLoad, utilCheckOut, commonUtilProp, gitProperties, jenkinsProp
def myGitAccess = 'cf6edfe6-5dc7-4bbf-b4f5-491e8e83b22b'
def branchToCheckOut = 'master'
def jenkinsRepo = 'https://github.com/manju1101/configurationRepo.git'


node{
    def gitCheckOutCalls
        stage('load git properties'){
            git branch: branchToCheckOut,
                credentialsId: myGitAccess, 
                url: jenkinsRepo

                gitProperties = readProperties  file: './Properties/gitlab.properties'
                jenkinsProp = readProperties  file: './Properties/JenkinsFile.properties'
                jenkinsGroovy = load './Properties/CheckOutGit.groovy'
                // load util repo
                utilRepo =jenkinsGroovy.checkOutRepo(gitProperties['GIT_UTIL_REPO'], branchToCheckOut,myGitAccess);
                reuseFunction = load "LinuxSystem/reUsableScripts.groovy"
                commonUtilProp = readProperties  file: './LinuxSystem/shellCommands.properties'
                
                println gitProperties['GIT_APP_POMFILE_PATH']
        }
        
        stage('Checkout SCM'){
            // Check out app repo
                jenkinsGroovy.checkOutRepo(gitProperties['GIT_APP_REPO'], branchToCheckOut,myGitAccess);
        }
        
        stage('Building Sonar....') {
            def project_path=gitProperties['GIT_APP_POMFILE_PATH']
            dir(project_path){
            //some block
                withSonarQubeEnv('SonarNameInJenkins') {
                    sh commonUtilProp['MVN_CLEAR_PACKAGE']+" "+commonUtilProp['SOANR_BUILD']
                }
            }
        }
        
        /*stage('upload') {
              script { 
                 def server = Artifactory.server 'Devops301Artifactory'
                 def uploadSpec = """{
                    "files": [{
                       "pattern": "target/*.war",
                       "target": "example-repo-local/Devops301_${env.BUILD_NUMBER}/"
                    }]
                 }"""

                 server.upload(uploadSpec) 
               }
        }*/
        
        stage('upload') {
            reuseFunction.uploadToArtifactory(jenkinsProp['ARTIFACTORY_ID'] ,jenkinsProp['ARTIFACTORY_PATTERN'],jenkinsProp['ARTIFACTORY_TARGET']  );
        }
        
        stage('Send email'){
            try{
                reuseFunction.triggerEmail();
            } catch (e) {
            currentBuild.result = "FAILED"
            reuseFunction.triggerEmail();
            throw e
          }
            
        }
    }
