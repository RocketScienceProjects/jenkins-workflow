node('Linux'){

    try {
      stage name: 'Init'
        def mvnHome = tool 'Linux-Maven'
        env.PATH = "${mvnHome}/bin:${env.PATH}"

     stage name: 'Code Checkout'
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: false, reference: '', shallow: true]], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])
        checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'jenks-scripts']], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/jenkins-workflow.git']]]

     stage name: 'Code Build'
        sh 'mvn clean deploy'

    stage name: 'Get the buildtime'
     }

    catch(err){
     stage name: 'Send Notification'
        currentBuild.result = 'FAILURE'
    }
}