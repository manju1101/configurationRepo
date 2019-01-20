def checkOutRepo(String repo, String branch, String credentials){
    git url: repo, 
        branch: branch, 
        credentialsId: credentials
}

def uploadScript(){
              script { 
                 def server = Artifactory.server 'Devops301Artifactory'
                 def uploadSpec = """{
                    "files": [{
                       "pattern": "target/*.war",
                       "target": "example-repo-local/Devops301_${env.BUILD_NUMBER}/"
                    }]
                 }"""
               }
}

return this
