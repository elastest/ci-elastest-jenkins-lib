//With classes
class elastest implements Serializable {
	private String ip
	private boolean shared
	private String lite
	
	def setShared(value){
		shared = value
	}
	
	def startElastest(){
		echo '[INI] startElastest'
		
		def start_elastest_result = sh script: 'docker run -d --name="elastest_platform" -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform start --forcepull '+ lite, returnStatus:true
		echo 'startElastest-- start_elastest_result = '+start_elastest_result
		
		echo '[END] startElastest'
		return (start_elastest_result==0)
	}
	
	def elastestIsRunning(){
		echo '[INI] elastestIsRunning'
		def platform_state = sh script: 'docker ps | grep elastest_platform | grep -c Up', returnStatus:true
		def etm_state = sh script: 'docker ps | grep etm_1 | grep -c Up', returnStatus:true
		echo '[END] elastestIsRunning'
		return (platform_state==0 && etm_state==0)
	}

	def waitElastest(){
		echo '[INI] waitElastest'
		def elastest_is_running = sh script: 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform wait', returnStatus:true
		echo '[END] waitElastest'
		return (elastest_is_running == 0)
	}

	def stopElastest(){
		echo '[INI] stopElastest'
		def start_elastest_result = sh script: 'docker run -d -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform stop', returnStatus:true
		echo 'start_elastest_result = '+start_elastest_result	
		echo '[END] stopElastest'
	}

	def getAPI(){
		echo '[INI] getAPI'
		def get_api = sh script: 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform inspect --api', returnStatus:true
		echo '[END] getAPI'
	}
	
	def elastest_pipeline(body) {
		echo '[INI] Main body of the library'
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
									
					def elastest_is_running = elastestIsRunning()
					echo "elastest_is_running? "+ elastest_is_running
					
					if (!elastest_is_running){
						echo 'ElasTest is not running...'
						echo 'START Shared ElasTest'
						startElastest()
						//lets set more time for waiting
						def counter = 3
						while (!elastest_is_running && counter > 0){
							elastest_is_running = waitElastest()
							counter = counter -1
							def return_status = sh script: 'docker ps | grep elastest_', returnStatus:true
							echo 'elastest_is_running:'+elastest_is_running
						}
							
						def return_status = sh script: 'docker ps | grep elastest_', returnStatus:true
						if (! elastest_is_running){
							currentBuild.result = 'FAILURE'
							return
						}
					}
					else {
						echo 'TODO: provide elastest feedback'
					}
				//body of the pipeline
				echo '[INI] User stages'
				body();	
				echo '[END] User stages'
				
				stage ('release elastest')
					echo ('Shared elastest wont be ende because other jobs would be using it')
			}
		}
		else {
			node('commonE2E'){
				stage ('launch elastest')
					echo "sharedElastest = ${SHARED_ELASTEST}"
					def elastest_is_running = elastestIsRunning()
					if (elastest_is_running){ //stop and start again --> elastest is unique and fresh with each start
						stopElastest()
						sleep(10)//TODO: change for method like waitToStop or something like that
						elastest_is_running = elastestIsRunning()
						echo 'elastest_is_running:'+elastest_is_running
						if (elastest_is_running){
							currentBuild.result = 'FAILURE'
							return
						}	
					}
					startElastest()
					//lets set more time for waiting
					def counter = 3
					while (!elastest_is_running && counter > 0){
						elastest_is_running = waitElastest()
						def return_status = sh script: 'docker ps | grep elastest_', returnStatus:true
						echo 'elastest_is_running:'+elastest_is_running
						counter = counter -1
					}

					if (! elastest_is_running){
						currentBuild.result = 'FAILURE'
						return
					}
				//body of the pipeline	
				echo '[INI] User stages'
				body();	
				echo '[END] User stages'
				
				stage ('release elastest')
					stopElastest()
			}
		}
		echo '[END] Main body of the library'
	}
	
}




