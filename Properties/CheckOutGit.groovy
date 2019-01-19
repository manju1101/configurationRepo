def checkOutRepo(String repo, String branch, String credentials){
    git url: repo, 
        branch: branch, 
        credentialsId: credentials
}

return this
