def call(body) {
	def config = [:] //values for configure the job
					 //in a future version of the components? by default latest
					 // 
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
	
	//check the execution ${sharedElastest}
	echo " \$ {config.sharedElastest} ${config.sharedElastest}" 
	echo " \$ config.sharedElastest $config.sharedElastest" 
	
	if ( '${config.sharedElastest}' ){
		node ('sharedElastest'){
			stage ('launch elastest')
				echo ('sharedElastest')
				echo ('retrieve scripts')
				
				sh 'if cd ci-elastest-jenkins-lib; then git pull; else git clone https://github.com/elastest/ci-elastest-jenkins-lib.git ci-elastest-jenkins-lib; fi'
				
				echo ('TODO: check if elastest is running')
				sh 'ls -ltr ci-elastest-jenkins-lib/scripts '
				def elastest_is_running = sh  script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py', returnStatus:true
				echo 'elastest_is_running = '+elastest_is_running
				
				if (elastest_is_running != 0 ){
					sh '#!/bin/bash -xe . ci-elastest-jenkins-lib/scripts/startElastest.sh'
				}
				else {
					echo 'TODO: provide elastest feedback'
				}
				
				
				
				
				
			//body of the pipeline
			body();	
			
			stage ('release elastest')
				echo ('TODO: check what to do with that...')
		}
	}
	else{
		node('commonE2E'){
			stage ('launch elastest')
				echo ('NOT sharedElastest')
				echo ('TODO: run elastest')
				echo ('TODO: provide elastest feedback')
				
			//body of the pipeline	
			body();
			
			stage ('release elastest')
				echo ('TODO: stop elastest')
				echo ('TODO: clean elastest')
		}
	}
}
