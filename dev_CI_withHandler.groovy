
		//This script is the main CI Pipeline. 

		/*
		Author: Niristotle Okram 
		*/

		/*
		Below are the features for this script:
		1. checks out the code from git with the target branch (branch where the metadata exist to deploy/validate)
		2. run the force-dev-tool to generate the src and the package.xml based on the changes (changes are determined by a git-diff between the target and the master branch)
		3. Optionally Validates the generated package
		4. Optionally deploys to the target env
		5. Creates a git tag and pushes up to the git repo - marking the SF deployment ID
		6. Send email notification on the status
		*/
    import groovy.json.JsonSlurperClassic
	import groovy.json.*
		 

	node('master') {

			def log = null;

		    def deployment_env = sf_user.substring(sf_user.lastIndexOf("com.") + 4, sf_user.length()).toUpperCase();
			println "Deploying to environment: $deployment_env"

		try{	
		    //delete workspace
		    deleteDir()
		       /*
		       echo the parameters that are being passed to the job
		       */
		       println "Target git branch: $target_branch"
		       println "Validation of the metadata set as: $validate"
		       //println "Deployment of the metadata set as: $deploy"
		       println "Specified sfdc_url for deployment: $sf_url"
		       println "Specified deployment user: $sf_user"
		       println "The release cycle version added: $release_version"  //this is used for the git tag of the package


			stage('tools') {
				antHome = tool 'default-ant'
				nodeHome = tool 'nodejs814'
				jdk = tool name: 'JAVA1.8'
				env.PATH="${env.PATH}:${nodeHome}/bin:${antHome}/bin"
				env.JAVA_HOME = "${jdk}"
			}

			stage('CodeCheckout') {
				checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], 
					userRemoteConfigs: [[url: 'ssh://tfs.cbre.com:22/tfs/CBRE01/SalesForce%20-%20Global%20Enterprise/_git/Global_Enterprise']]])
			}
			stage('Generate_Src&Package'){
				sh '''node -v
				npm list -g
				git fetch --all
				git branch -a
				git checkout master
				git checkout $target_branch
				git diff --ignore-space-at-eol master $target_branch | force-dev-tool changeset create devDeploy
				mv src src_bk
				mkdir src
				mv config/deployments/devDeploy/* src/
				#this will handle if any destructive xml get generated
				echo "Check if destructive xml exist.........."
		        if [ -f src/destructiveChanges.xml ]
		        then
		           (
		            mv src/destructiveChanges.xml src/destructiveChanges.xml.src_bk
		            status=$?
		            if [ $status == 0 ]
		            then
		             echo "renamed the destructiveChanges.xml to destructiveChanges.xml.bk, to avoid interfering in the deployment via the API....."
		            else
		             echo "renaming failed for the destructiveChanges.xml"
		             exit 1
		            fi  
		           )
		        else
		            echo "No destructiveChanges.xml was generated......"
		        fi     
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
		        //validate = $validate  
				if("${validate}" == "true") {
					sh 'ant -f build/build.xml deployEmptyCheckOnly -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass'
				} else {
					println "Validation Skipped........"
				}
			}

			stage('Deploy') {
				//redirect output to file and tail the file 
				//if("${deploy}" == "true") {
				log = sh (returnStdout: true, script: "ant -f build/build.xml deployCode -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass").trim()
				println "${log}"

				//parse the String for the ID
				def sub = { it.split("current deploy task: ")[1][0..15] } //Chnage this to the right one for deploy
				def tag = sub(log)
				println tag /* else {
					println "Deployment Skipped and performing only Validation........"
				}
				*/

			stage('Archive') {
				sh 'git add -A'
				sh 'git commit -m "Pushing the deployed code from build $BUILD_NUMBER"'
				sh "git tag $release_version.$BUILD_NUMBER-$tag"
				sh "git push origin $release_version.$BUILD_NUMBER-${tag}"
				}

			stage('success_notify'){

				emailext (
		        	subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' in SFDC Org: '${deployment_env}'",
		        	body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' was <strong>successfully</strong> executed.<br/><br/><strong>This is console log: </strong> <br/> '${log}'<br/><br/> Deployed and archived SF deployment ID: <strong>'${tag}'</strong> to SalesForce Org: <strong>'${deployment_env}'</strong> . <br />The Archive is: <strong>'$release_version.$BUILD_NUMBER-$tag'</strong><br/>Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
		        	mimeType: 'text/html',
		        	attachLog: true,
		        	compressLog: true,
		        	recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
		        	to: "niristotle.okram@cbre.com"
		        	)
				}

			}

		}

		catch(err){
			stage('failure_handler') {
			echo "Exception Caught: ${err}"
			//getBuildDetails()
			if(buildDetails.abortedByUser) {
				currentBuild.result = 'UNSTABLE'
				println "Not sending Email notice"
			} else {
				currentBuild.result = 'FAILURE'
				emailext (
		          subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'  in SFDC Org: '${deployment_env}'",
		          body: """<h1>Automated email notification from Jenkins CI system:</h1><p>The job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' execution <strong>failed, while trying to deploy in '${deployment_env}'!!!</strong> <br /><br /><br /><strong>Deployment failure log from SFDC API:</strong><br /> '${log}'<br /><br />Attached is the build log.&nbsp;</p><p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
		          mimeType: 'text/html',
		          attachLog: true,
		          compressLog: true,
		          recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider']],
		          to: "niristotle.okram@cbre.com"
		          )

			}

		   } 
	    }

	}


def fetchBuildDetails() {
    url1 = env.BUILD_URL + 'wfapi/describe'
    url2 = env.BUILD_URL + 'api/json?depth=2'
    jsonResponse = sh(script: 'echo "{\\"wfapi\\":`curl --silent --user us_svc_jenkinsCI:4266cfedd0720dab4a3744eccfaac3a4 --netrc-file . '+url1+'`,\\"api\\":`curl --silent --user us_svc_jenkinsCI:4266cfedd0720dab4a3744eccfaac3a4 --netrc-file . '+url2+'`}"', returnStdout: true).trim()
    echo "Fetched build details:"
    //echo prettyPrint(jsonResponse)
    return new JsonSlurperClassic().parseText(jsonResponse)
}
 

def getBuildDetails() {
    def buildDetails = fetchBuildDetails()
    buildDetails.shouldNotificationGetSentToInfrastructureTeam = false
    buildDetails.abortedByUser = false

    // checking if user aborted the build
    for(int i=0; i < buildDetails.api.actions.size(); i++) {
        if (buildDetails.api.actions[i]._class == 'jenkins.model.InterruptedBuildAction') {
            if (buildDetails.api.actions[i].causes[0]._class == 'jenkins.model.CauseOfInterruption$UserInterruption') {
                buildDetails.abortedByUser = true
            }
        }
    }

    // // checking name of failed stage and deciding if in-team should get mentioned in slack message
    // for(int i=0; i < buildDetails.wfapi.stages.size(); i++) {
    //     if (buildDetails.wfapi.stages[i].status == 'FAILED') {
    //         def failedStageName = buildDetails.wfapi.stages[i].name.toLowerCase()
    //         if (
    //             failedStageName.indexOf("checkout") >= 0
    //             || failedStageName.indexOf("build") >= 0
    //             || failedStageName.indexOf("deploy") >= 0
    //         ) {
    //             buildDetails.shouldNotificationGetSentToInfrastructureTeam = true
    //         }
    //     }
    // }

    echo "Build details enchanced:"
    //println prettyPrint(toJson(buildDetails))

    return buildDetails
}
