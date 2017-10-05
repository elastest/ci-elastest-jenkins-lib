def startElastest(){
	def start_elastest_result = sh  script: 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform start  --forcepull --nocheck', returnStatus:true
	echo 'start_elastest_result = '+start_elastest_result

	if (start_elastest_result == 0){
		elastest_is_running = sh  script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py', returnStatus:true
		echo 'elastest_is_running = '+elastest_is_running
	}
	else {
		def stop_elastest_result = sh  script: 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform stop --forcepull --nocheck', returnStatus:true
	}
	
	return start_elastest_result
}

def checkETM(){
	sh 'if cd ci-elastest-jenkins-lib; then git pull; else git clone https://github.com/elastest/ci-elastest-jenkins-lib.git ci-elastest-jenkins-lib; fi'
	sh 'ls -ltr ci-elastest-jenkins-lib/scripts '
	def elastest_is_running = sh  script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py', returnStatus:true
	
	return (elastest_is_running == 0)
}


def call(body) {
	def config = [:] //values for configure the job
					 //in a future version of the components? by default latest
					 // 
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
	
	//check the execution ${sharedElastest}
		
		
	if ( "$SHARED_ELASTEST" ){
		node ('sharedElastest'){
			stage ('launch elastest' )			
				echo "sharedElastest= $SHARED_ELASTEST"
				echo ('retrieve scripts')
				sh 'if cd ci-elastest-jenkins-lib; then git pull; else git clone https://github.com/elastest/ci-elastest-jenkins-lib.git ci-elastest-jenkins-lib; fi'
				
				def elastest_is_running = checkETM()
							
				if (elastest_is_running){
					echo 'ElasTest is not running...'
					echo 'START Shared ElasTest'
					elastest_is_running = startElastest()
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
				echo ('sharedElastest=$SHARED_ELASTEST')
				echo ('TODO: run elastest')
				def elastest_is_running = startElastest()
				
				echo ('TODO: provide elastest feedback')
				
			//body of the pipeline	
			body();
			
			stage ('release elastest')
				echo ('TODO: stop elastest')
				echo ('TODO: clean elastest')
		}
	}
}
