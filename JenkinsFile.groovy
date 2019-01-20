def utilRepo, jenkinsGroovy, reuseFunction, gitLoad, utilCheckOut, commonUtilProp, gitProperties
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
                 jenkinsGroovy.uploadScript();
                 server.upload(uploadSpec) 
               }
        }
        
        stage('Create docker tomcat image') {
            script {
            //   docker.build registry + ":$BUILD_NUMBER"
            // docker rmi tomcat
            docker.image(commonUtilProp['TOMCAT_IMAGE']).pull();
            sh 'docker run -d --name '+commonUtilProp['TOMCAT_LOCAL_NAME']+' -p '+commonUtilProp['TOMCAT_HOST_PORT']+' '+
            commonUtilProp['TOMCAT_IMAGE']
            }
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

