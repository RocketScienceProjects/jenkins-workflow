node("master") {

	def pr_branch = null

	try{
		deleteDir()

		stage('tools') {
			antHome = tool 'default-ant'
			nodeHome = tool 'nodejs814'
			jdk = tool name: 'JAVA1.8'
			env.PATH="${env.PATH}:${nodeHome}/bin:${antHome}/bin"
			env.JAVA_HOME = "${jdk}"
		}

		stage('CodeCheckout') {
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [], 
            submoduleCfg: [], userRemoteConfigs: [[name: 'origin', refspec: '+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin-pull/*', 
            url: 'ssh://tfs.cbre.com:22/tfs/CBRE01/SalesForce%20-%20Global%20Enterprise/_git/Global_Enterprise']]]
		}


		stage('CreateLocalPR') {

			//log = sh (returnStdout: true, script: "ant -f build/build.xml deployCode -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass").trim()
			log = sh ( returnStdout: true, script: '''
			pr=`git branch -r -v --no-abbrev --contains $(git rev-parse HEAD)`
			echo "Validating the Pull request branch: $pr"	
			git checkout -b PRbranch
			echo "created the pr branch"
			git checkout dev
			echo "checkout the dev branch"
			git branch''' ).trim()
			println "${log}"

			def sub = { it.split("Validating the Pull request branch: ")[1][0..23] } //Chnage this to the right one for deploy
			pr_branch = sub(log)
			println "The PR branch is: $pr_branch" 

		
		stage('Generate_src_xml') {
            sh '''node -v
			git fetch --all
			git branch -a
			git diff --ignore-space-at-eol dev PRbranch | force-dev-tool changeset create pr_validate
			mv src src_bk
			mkdir src
			mv config/deployments/pr_validate/* src/
			echo "Check for permissionSets and do file manipulation"
			if [ -d src/permissionsets ] 
			  then
				(
              		cd src && for i in permissionsets/*
               		do
                	##### yes | cp -f ../src_bk/"$i" "$i"
                	cp -f ../src_bk/"$i" "$i"
                	status=$?
                	if [ $status == 0 ]
                	then
                	  echo "copied ../src_bk/$i into src/$i"
                    else
                  		echo "failed to copy ../src_bk/$i into src/$i"
                  	exit 1
                	fi
              		done;
				)
			else
			echo "No permissionsets components found to do file manipulation for deployments......... Using the default src created by force-dev-tool"
		fi
		'''
		}
  
        stage('Validate') {
        //runs a dry run deployment of the changes in the PR 
			sh 'ant -f build/build.xml PR_deployEmptyCheckOnly -Dsfdc.serverurl=https://test.salesforce.com/ -Dsfdc.username=sfdc.deploy@cbre.com.devr39 -Dsfdc.password=CBREPassword1~!'
	    }

	    stage('success_notify'){

		emailext (
        	subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
        	body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' was <strong>successfully</strong> executed. <br /><br />The PR branch: <strong>${pr_branch}</strong> have passed the validation. And is ready for the code review by the reviewer.</strong><br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        	mimeType: 'text/html',
        	attachLog: true,
        	compressLog: true,
        	recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
        	to: "niristotle.okram@cbre.com,Muhammad.Malik@cbre.com,Baljit.Kaur@cbre.com,James.Royle@cbre.com,Salman.Sheikh@cbre.com"
        	)
		}
	}

	}	
	

	catch(err){
		stage('failure_handler') {

		echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'

        emailext (
          subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
          body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' <strong>FAILED</strong>. <br /><br />The PR branch: <strong>${pr_branch}</strong> failed the validation, owner should revisit the changes.</strong><br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
          mimeType: 'text/html',
          attachLog: true,
          compressLog: true,
          recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
          to: "niristotle.okram@cbre.com,Muhammad.Malik@cbre.com,Baljit.Kaur@cbre.com,James.Royle@cbre.com,Salman.Sheikh@cbre.com"
          )
	}

	}
}
