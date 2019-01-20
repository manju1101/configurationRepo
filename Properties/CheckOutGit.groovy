def checkOutRepo(String repo, String branch, String credentials){
    git url: repo, 
        branch: branch, 
        credentialsId: credentials
}

def uploadSpec = """{
                    "files": [{
                       "pattern": "target/*.war",
                       "target": "example-repo-local/Devops301_${env.BUILD_NUMBER}/"
                    }]
                 }"""

return this
