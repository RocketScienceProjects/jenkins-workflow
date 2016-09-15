println('Loaded notify.gsh')

def notifyFailed() {
  emailext (to: "${EMAIL}",
      subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: "<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'</p> <p>Check console output at &quot;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&quot;</p>",
        recipientProviders: [[$class: 'CulpritsRecipientProvider']],
        mimeType:'text/html'
    )
}

return this;
//this is test comment
