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
				
				sh 'git clone https://github.com/elastest/ci-elastest-jenkins-lib.git'
				
				sh 'pwd'
				sh 'cd ci-elastest-jenkins-lib && ls'
				
				echo ('TODO: check if elastest is running')
				elastest_is_running = sh ( script: 'python ci-elastest-jenkins-lib/scripts/checkETM.py',
								 returnStdout: true).trim()
								
				if (elastest_is_running != 0 ){
					sh '. ./ci-elastest-jenkins-lib/scripts/startElastest.sh'
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
