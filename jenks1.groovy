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
        //step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']])])
        emailext body: 'A Test EMail', recipientProviders: [[$class: 'CulpritsRecipientProvider']], subject: 'Test', to: 'niristotle.okram@infosys.com'
    }
}
