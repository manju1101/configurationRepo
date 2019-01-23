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
        
        stage('upload') {
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
        }
        
        /*stage('upload') {
            reuseFunction.uploadToArtifactory(jenkinsProp['ARTIFACTORY_ID'] ,jenkinsProp['ARTIFACTORY_PATTERN'],jenkinsProp['ARTIFACTORY_TARGET']+env.BUILD_NUMBER+'/'  );
        }*/
        
        stage('create temp volume') {
            println jenkinsProp['DOCKER_NAME']
                sh 'docker stop '+jenkinsProp['DOCKER_NAME']
                sh 'docker rm '+jenkinsProp['DOCKER_NAME']
               sh 'sudo rm -rf /tmp/webapps'
                sh 'mkdir /tmp/webapps'
                sh 'cp ./target/*.war /tmp/webapps/' 
        }
        
        stage('Building tomcat image') {
            //   docker.build registry + ":$BUILD_NUMBER"
            // docker rmi tomcat
            docker.image("tomcat:latest").pull();
            sh 'docker run -d --name '+jenkinsProp['DOCKER_NAME']+' -p '+jenkinsProp['DOCKER_PORT']+' -v /tmp/webapps/:/usr/local/tomcat/webapps/:rw '+jenkinsProp['DOCKER_IMAGE']
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
