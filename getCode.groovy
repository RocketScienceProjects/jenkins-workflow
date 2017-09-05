//This pipeline script will perform sf:retrive and checkin code to vcs. 

/*
Author: Niristotle Okram 
*/

node('master') {

    println "Selected default/source branch: $default_branch"
    println "User input for commit message in git: $commit_msg" 
    println "sf server URL selected: $sf_url"
    println "sf username entered: $sf_user"


	try{

		//delete workspace
        deleteDir()

        time_stamp = sh (returnStdout: true, script: 'date "+%Y%m%d_%H%M%S"').trim()

    	println "The time stamp for this build is: $time_stamp"

    	/*
			testing
    	*/
    	def name = "okram"

        stage('tools') {
			antHome = tool 'default-ant'
			jdk = tool name: 'JAVA1.8'
			env.PATH="${env.PATH}:${antHome}/bin"
			env.JAVA_HOME = "${jdk}"
		}

        //checkout repository with the default branch
        stage('CodeCheckout') {
			checkout([$class: 'GitSCM', branches: [[name: "*/$default_branch"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], 
				userRemoteConfigs: [[url: 'ssh://tfs.cbre.com:22/tfs/CBRE01/SalesForce%20-%20Global%20Enterprise/_git/Global_Enterprise']]])
		}

		stage('createBranch'){
			withEnv(["time=${time_stamp}"]){
			sh '''
			echo "Value of time_stamp set as env variable is: $time"
			git branch
			git fetch origin
			git checkout $default_branch
			echo "checked out $default_branch"
    		git checkout -b configChanges/${default_branch}_"$time"
    		echo "checked out a new branch: configChanges/${default_branch}_$time"
			echo "Printing the git branch output:    "
			git branch 
			'''
			}
		}

		stage('getCode') {
			sh 'ant -f build/build.xml getCode -Dsfdc.serverurl=$sf_url -Dsfdc.username=$sf_user -Dsfdc.password=$sf_pass'
		}

		stage('PushChanges'){
			withEnv(["time=${time_stamp}"]){
			sh ''' 
			git config user.name jenkinsSvc
			git config user.email noreply@sf.cbre.com
			# add, commit and push changes
			filelist=`git status -s`
				if [[ -n $filelist ]]; then
  					git add src/*
  					git commit -a -m "$commit_msg $filelist"
 					returnval=`git push origin configChanges/${default_branch}_"$time"`
 						if [[ -z $returnval ]]; then
 							exit 0;
 						else
 							exit 1;
 						fi
				else
					echo '**** Nothing to commit ****'
				exit 0
				fi
			'''
			}
		}




	}
	catch(err){
		currentBuild.result = 'FAILURE'
		println "there was a failure"

	}
}
