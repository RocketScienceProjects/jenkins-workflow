#!groovy

node('master') {
    def mvnHome = tool 'M3'
  stage 'git clone'
    checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])

 // stage 'build'
   // bat 'mvn clean deploy'
   
  stage 'Junit Test & Archieving Result'
   //this phase is added to execute prior to performing release
   bat "mvn clean test"
   //archieve the junit test result
   step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: ".\\target\\surefire-reports\\*.xml"])
   
  
  //Adding the below to test the release goal
  stage 'Cut_Release'
    def pom = readMavenPom file: 'pom.xml'
    def version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}")
    //test if the parameters are being imported at the run time
    //def releaseVersion = ${releaseVersion}
    //def developmentVersion = ${developmentVersion}
    //bat "mvn -B -DdevelopmentVersion=${developmentVersion} -DreleaseVersion=${releaseVersion} -DpushChanges=false -DlocalCheckout=true -DpreparationGoals=initialize release:prepare release:perform"
    bat "mvn -DreleaseVersion=${version} -DdevelopmentVersion=${pom.version} -DpushChanges=false -DlocalCheckout=true -DpreparationGoals=initialize release:prepare release:perform -B"
    input 'Publish?'
     
  stage 'publish git tag'
    //bat "git push origin ${pom.artifactId}-${releaseVersion}"
    bat "git push origin ${pom.artifactId}-${version}"
    

}
