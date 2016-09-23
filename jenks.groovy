package org.jenkinsci.plugins.workflow.support.steps.build;

import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.security.ACL;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction;






def notify

node('Linux'){

    try{
      stage name: 'Init'
        //def mvnHome = tool 'M3'
        def mvnHome = tool 'Linux-Maven'

     stage name: 'Code Checkout'
        //Checkout the source code
        //checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 2]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: false, reference: '', shallow: true]], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/BlueKing.git']]])

        //checkout the jenkins workflow script
        //checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'jenks-scripts']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/jenkins-workflow.git']]]
        checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: 'jenks-scripts']], gitTool: 'Linux_Git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c38cbe8-f4e2-4150-918a-0264a28cff73', url: 'git@github.com:RocketScienceProjects/jenkins-workflow.git']]]
    stage name: 'Load groovy scripts'
        notify = load 'jenks-scripts/notify.groovy'

     stage name: 'Code Build'
        //bat "${mvnHome}/bin/mvn -B clean deploy"
        sh "${mvnHome}/bin/mvn -B clean deploy"
  //   stage name: 'Publish Test Data'
    //    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
     //stage name: 'Publish Coverage Report'
        //step([$class: 'JacocoPublisher', sourcePattern: 'src/com/blueking/controller/*.java'])
     /*
     stage name: 'Code Coverage Report'
       step([$class: 'JacocoPublisher', sourcePattern: '**/src/**.java'])
    */

    stage name: 'Get the buildtime'

    //def buildtime = currentBuild.getDuration();
    //println "This is the build time: ${buildtime} "
    }
    catch(e){
     stage name: 'Send Notification'
        currentBuild.result = 'FAILURE'
        throw e
        notify.notifyFailed()
    }
}
