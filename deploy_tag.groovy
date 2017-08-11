//This script deploys a particular git tag to a target SF Organization - it takes certain env specific info as the job parameters

/*
Author: Niristotle Okram 
*/

/*
Below are the features for this script:
1. checks out the git-tag from git to Validate/Deploy
2. Optionally Validates the generated package
3. Optionally deploys to the target env
4. Send email notification on the status
*/




node('master') {


    println "Git tag selected to deploy: $git_tag"
    println "Validation of the metadata set as: $validate"
    println "Deployment of the metadata set as: $deploy"
    println "Deploying to Org: $sf_url"
    println "Deploying using the deployment user: $sf_user"

	try {
		deleteDir()

		stage('tools') {
			antHome = tool 'default-ant'
			nodeHome = tool 'nodejs814'
			jdk = tool name: 'JAVA1.8'
			env.PATH="${env.PATH}:${nodeHome}/bin:${antHome}/bin"
			env.JAVA_HOME = "${jdk}"
	    }

	    stage('CodeCheckout') {
	    	checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: "tags/$git_tag"]], doGenerateSubmoduleConfigurations: false, extensions: [], 
	    	submoduleCfg: [], userRemoteConfigs: [[url: 'ssh://tfs.cbre.com:22/tfs/CBRE01/SalesForce%20-%20Global%20Enterprise/_git/Global_Enterprise']]]
	    }

	    stage('Validate') {
	    	try {
	    		if("${validate}" == "true") {
					sh "ant -f build/build.xml deployEmptyCheckOnly -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass"
				} else {
					println "Validation Skipped as opted, in the job trigger........"
				}
		    }
		    catch(err) {
		    	echo "Caught: ${err}"
        		currentBuild.result = 'FAILURE'

        	emailext (
        		subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        		body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' execution <strong>failed!!!</strong> during code <strong>Validation</strong><br /> Exception thrown by the job is: ${err} <br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        		mimeType: 'text/html',
        		attachLog: true,
        		compressLog: true,
        		recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
        		to: "niristotle.okram@cbre.com"
        		)

        		//Print err and exit the pipeline
        		error 'Build Failed, look into the build log for more details'

		    }

	    }

	    stage('Deploy') {
	    	//sh 'ant -f build/build.xml deployCode -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=${$sf_pass}'
	    	try{
	    	 if("${deploy}" == "true") {
	    		log = sh (returnStdout: true, script: "ant -f build/build.xml deployCode -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass").trim()
				println "${log}"

				//parse the String for the ID
				def sub = { it.split("current deploy task: ")[1][0..15] } //Chnage this to the right one for deploy
				def tag = sub(log)
				println "The deployment of git tag: $git_tag is now set to a deployment id $tag"
		     } else {
		    	println "Deployment Skipped as opted, in the job trigger........"
		     }
		    }

		    catch(err) {
		    	echo "Caught: ${err}"
        		currentBuild.result = 'FAILURE'

        	emailext (
        		subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        		body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' execution <strong>failed!!!</strong> during metadata <strong>Deployment</strong><br /> Exception thrown by the job is:  ${err} <br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        		mimeType: 'text/html',
        		attachLog: true,
        		compressLog: true,
        		recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
        		to: "niristotle.okram@cbre.com,Muhammad.Malik@cbre.com,Baljit.Kaur@cbre.com,James.Royle@cbre.com,Salman.Sheikh@cbre.com"
        		)

        	    //Print err and exit the pipeline
        		error 'Build Failed, look into the build log for more details'
		    }
		}

		stage('notify') {

			emailext (
        		subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        		body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' was <strong>successfully</strong> executed.  <br /> <br /><strong> Deployed the git tag: '$git_tag' to the target: '$sf_url' by the user: '${sf_user}' </strong> <br /><br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        		mimeType: 'text/html',
        		attachLog: true,
        		compressLog: true,
        		recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
        		to: "niristotle.okram@cbre.com,Muhammad.Malik@cbre.com,Baljit.Kaur@cbre.com,James.Royle@cbre.com,Salman.Sheikh@cbre.com"
        		)
		}



	}

	catch(err) {
		stage('failure_notice') {
			echo "Caught: ${err}"
        	currentBuild.result = 'FAILURE'

        	emailext (
        		subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        		body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' execution <strong>failed!!!</strong> <br /> Exception thrown by the job is: ${err} <br /><strong> Failed to deploy the git tag: '$git_tag' to the target: '$sf_url' by the user: '${sf_user}' </strong> <br /><br /> <br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        		mimeType: 'text/html',
        		attachLog: true,
        		compressLog: true,
        		recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
        		to: "niristotle.okram@cbre.com,Muhammad.Malik@cbre.com,Baljit.Kaur@cbre.com,James.Royle@cbre.com,Salman.Sheikh@cbre.com"
        		)
		}
	}
}
