def call(body) {
	def config = [:] //values for configure the job
					 //in a future version of the components? by default latest
					 // 
	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
	
	//check the execution ${sharedElastest}
	
	if (${sharedElastest}){
		node ('sharedElastest'){
			stage ('launch elastest')
				echo ('sharedElastest')
				echo ('TODO: check if elastest is running')
				def elastest_is_running = false
				if (! elastest_is_running ){
					echo ('TODO: run elastest')
				}
				else {
					echo ('TODO: provide elastest feedback')
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
