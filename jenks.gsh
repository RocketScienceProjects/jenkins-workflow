
node('master'){

    try{
     stage name: 'Init'
        def mvnHome = tool 'M3'
     stage name: 'Git Clone'
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 2]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])
     stage name: 'Code Build'
        bat "${mvnHome}/bin/mvn -B clean deploy"
     stage name: 'Publish Test Data'
        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
     //stage name: 'Publish Coverage Report'
        //step([$class: 'JacocoPublisher', sourcePattern: 'src/com/blueking/controller/*.java'])
    }
    catch(e){
     stage name: 'Send Notification'
        currentBuild.result = 'FAILURE'
        emailext (to: "${EMAIL}",
            subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
            body: "<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'</p> <p>Check console output at &quot;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&quot;</p>",
              recipientProviders: [[$class: 'CulpritsRecipientProvider']],
              mimeType:'text/html')
    }
}
