/*
* Method to start ElasTest with the default method: docker image.
* In case that some arises the method will try to stop all the ElasTest components that had been started
*/
def startElastest(){
	def little = ''
	if ("$ELASTEST_LITTLE"=='true'){
		little = '--little'
	}
	
	echo '[INI] startElastest'
	def start_elastest_result = sh script: 'docker run -d --name="elastest_platform" -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform start  --forcepull --nocheck '+ little, returnStatus:true
	echo 'startElastest-- start_elastest_result = '+start_elastest_result
	
	sh script: 'docker ps', returnStatus: true
	def condition = sh script: 'docker ps | grep etm_1 | grep -c Up', returnStatus:true, returnStdout:true 
	
	echo 'startElastest-- Condition: '+condition
	
	//give the component time to start 
	counter = 90
	
	while ( condition == '0' ) { 
		echo 'startElastest-- inside the while'
		sleep (2000)
		counter = counter -1
		if (counter == 0){
			echo "startElastest-- Timeout while wait for ETM started"
			start_elastest_result = -1
			break
		}
		condition = sh script: 'docker ps | grep etm_1 | grep -c Up', returnStdout:true
		echo '\t startElastest-- Condition: '+condition
	}
	
	echo 'startElastest-- start_elastest_result='+start_elastest_result
	
	if (start_elastest_result == 0){
		elastest_is_running = sh script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py', returnStatus:true
		echo 'startElastest-- elastest_is_running = '+ (elastest_is_running==0)
	}
	else {
		echo 'startElastest-- stop platform as the etm has not been launched'
		def stop_elastest_result = sh script: 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform stop --forcepull --nocheck', returnStatus:true
	}
	
	echo '[END] startElastest'
	return (elastest_is_running==0)
}

def checkETM(){
	sh 'if cd ci-elastest-jenkins-lib; then git pull; else git clone https://github.com/elastest/ci-elastest-jenkins-lib.git ci-elastest-jenkins-lib; fi'
	sh 'ls -ltr ci-elastest-jenkins-lib/scripts '
	def elastest_is_running = sh  script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py', returnStatus:true
	
	return (elastest_is_running == 0)
}

def stopElastest(){
	def start_elastest_result = sh script: 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform stop  --forcepull --nocheck', returnStatus:true
	echo 'start_elastest_result = '+start_elastest_result	
}


def call(body) {
	def config = [:] //values for configure the job
					 //in a future version of the components? by default latest
					 // 
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
	
	//check the execution ${sharedElastest}
		
		
	if ( "$SHARED_ELASTEST" == 'true' ){
		node ('sharedElastest'){
			stage ('launch elastest' )			
				echo "sharedElastest = ${SHARED_ELASTEST}"
				echo ('retrieve scripts')
				
				sh 'if cd ci-elastest-jenkins-lib; then git pull; else git clone https://github.com/elastest/ci-elastest-jenkins-lib.git ci-elastest-jenkins-lib; fi'
				
				def elastest_is_running = checkETM()
				echo "elastest_is_running? "+ elastest_is_running
				
				if (!elastest_is_running){
					echo 'ElasTest is not running...'
					echo 'START Shared ElasTest'
					elastest_is_running = startElastest()
					if (! elastest_is_running){
						currentBuild.result = 'FAILURE'
					}
				}
				else {
					echo 'TODO: provide elastest feedback'
				}
			//body of the pipeline
			body();	
			
			stage ('release elastest')
				echo ('Shared elastest wont be ende because other jobs would be using it')
		}
	}
	else {
		node('commonE2E'){
			stage ('launch elastest')
				echo "sharedElastest = ${SHARED_ELASTEST}"
				def elastest_is_running = startElastest()
				if (! elastest_is_running){
						currentBuild.result = 'FAILURE'
				}
				echo ('TODO: provide elastest feedback')
				
			//body of the pipeline	
			body();
			
			stage ('release elastest')
				stopElastest()
		}
	}
}
