//def notify

node('Linux'){

    try {
      stage name: 'Init'
        //def mvnHome = tool 'M3'
        def mvnHome = tool 'Linux-Maven'
        env.PATH = "${mvnHome}/bin:${env.PATH}"

     stage name: 'Code Checkout'
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: false, reference: '', shallow: true]], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])
        checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'jenks-scripts']], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/jenkins-workflow.git']]]

  //  stage name: 'Load groovy scripts'
    //    notify = load 'jenks-scripts/notify.groovy'

     stage name: 'Code Build'
        //bat "${mvnHome}/bin/mvn -B clean deploy"
        sh 'mvn -B clean deploy'
        sh 'echo $?'
  //   stage name: 'Publish Test Data'
    //    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
     //stage name: 'Publish Coverage Report'
        //step([$class: 'JacocoPublisher', sourcePattern: 'src/com/blueking/controller/*.java'])
     /*
     stage name: 'Code Coverage Report'
       step([$class: 'JacocoPublisher', sourcePattern: '**/src/**.java'])
    */

    stage name: 'Get the buildtime'
     //println "this is buildtime stage"
    //def buildtime = currentBuild.getDuration();
    //println "This is the build time: ${buildtime} "
    }

    catch(err){
     stage name: 'Send Notification'
        currentBuild.result = 'FAILURE'
        //throw err;
        //notify.notifyFailed()
    }
}
