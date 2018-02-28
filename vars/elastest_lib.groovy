//With classes
class elastest_lib implements Serializable {
	
	//Some Contstants
	private experimental = "--mode=experimental"
	private experimental_lite = "--mode=experimental-lite"
	
	private String elastest_docker_start = 'docker run -d --name="elastest_platform" -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform:'

	//configuration of the library
	private boolean verbose = false //if the library should echo debug information 
	private boolean shared = false //if the ElasTest instance is shared
	private boolean is_Authenticated = false
	private boolean with_ere = false
	

	def ctx //context of the executing pipeline

	//info of the ElasTest
	private String ip= ""
	private String port="37000"
	
	//authentication info
	private String elastest_user = "elastest"
	private String elastest_pass = ""

	//parameters for the ElasTest
	private String mode = '' //default normal
	private String version='latest'
	private String ere_version = 'latest'

	
	
	/*
	* User methods: Methods to be used in the pipeline to interact with ElasTest
	*/
	/*
	*	@return elastest_user
	*/
	def getElasTestUser() { return this.@elastest_user }
	
	/*
	*	@return elastest_pass
	*/
	def getElasTestPassword() { return this.@elastest_pass }
	
	/*
	*	@return elastest_ip
	*/
	def getIp() { return this.@ip }

	/*
	*	@return elastest_port
	*/
	def getPort() { return this.@port }
	
	/*
	*	@ return full conection url for torm
	*/
	def getEtmUrl() { 
		return 'http://'+this.@ip+':'+this.@port 
	}
	
	/*
	*	When using methods of this library inside any provided step should be called through the 
	*	returned object by this method
	*	@return this object (executable library)
	*/
	def getElastestMethods(){ return this }
	
	
	/*
	*	Connect container to elastest network
	*/
	def connect2ElastestNetwork(){
		echo '[INI] connect2ElastestNetwork'
		if (! this.@shared){
			def containerId= this.@ctx.sh (
				script: 'cat /proc/self/cgroup | grep "docker" | sed s/\\\\//\\\\n/g | tail -1',
				returnStdout: true
			).trim() 
			echo "containerId = ${containerId}"
			this.@ctx.sh "docker network connect elastest_elastest "+ containerId
		}
		echo '[END] connect2ElastestNetwork'
	}
	
	
	def pullERE(){
		return pullERE(this.@version)
	}
	/*
	* force to retrieve ere into environment.
	*	
	*/
	def pullERE(String ere_version){
		echo '[INI] pullERE'
		if (! this.@shared){
			echo "login in ecr"
			def login_str = this.@ctx.sh script: 'aws ecr get-login --no-include-email --region eu-west-1',
						  returnStdout: true
			this.@ctx.sh login_str		
			
			echo "pulling ERE"
			this.@ctx.sh "docker pull 842800759158.dkr.ecr.eu-west-1.amazonaws.com/elastest/elastest-recommendation-engine:"+ere_version			
		}
		echo '[END] pullERE'
	}
	/*
	*	predefined pipeline for executing e2e test over a managed ElasTest platform
	*/
	def pipeline(body) {
		
		def config = [:] //values for configure the job
						 //in a future version of the components? by default latest
						 // 
		body.resolveStrategy = Closure.DELEGATE_FIRST
		body.delegate = config
		
		if (this.@shared == true ){
			this.@ctx.node('docker'){
				this.@ctx.stage ('launch elastest')
				
					echo "sharedElastest ="+this.@shared
					//echo "SHARED_ELASTEST_IP: $SHARED_ELASTEST_IP"

					def elastest_is_running = testRemoteElastest()
					if (! elastest_is_running){
						this.@ctx.currentBuild.result = 'FAILURE'
						return
					}
					else {
							initializeApi()
					}
					
				//body of the pipeline	
				echo '[INI] User stages'
				body();	
				echo '[END] User stages'
				
				this.@ctx.stage ('release elastest')
					stopElastest()
			}
		}
		else {
			this.@ctx.node('commonE2E'){
				this.@ctx.stage ('launch elastest')
					echo "sharedElastest ="+this.@shared
					def elastest_is_running = elastestIsStuck() // || elastestIsRunning() not necessary as stuck will also check if it is running
					if (elastest_is_running){ //stop and start again --> elastest is unique and fresh with each start
						stopElastest()
						sleep(10)//TODO: change for method like waitToStop or something like that
						elastest_is_running = elastestIsRunning()
						echo 'elastest_is_running:'+elastest_is_running
						if (elastest_is_running){
							this.@ctx.currentBuild.result = 'FAILURE'
							return
						}	
					}
					startElastest()
					//lets set more time for waiting
					elastest_is_running = waitElastest();
					def counter = 3
					while (!elastest_is_running && counter > 0){
						elastest_is_running = waitElastest()
						def return_status = this.@ctx.sh script: 'docker ps | grep elastest_', returnStatus:true
						echo 'elastest_is_running:'+elastest_is_running
						counter = counter -1
					}

					if (! elastest_is_running){
						this.@ctx.currentBuild.result = 'FAILURE'
						return
					}
					else {
							initializeApi()
					}
					
				//body of the pipeline	
				echo '[INI] User stages'
				body();	
				echo '[END] User stages'
				
				this.@ctx.stage ('release elastest')
					stopElastest()
			}
		}
	}
	
	/*
	*	Initialize unique instance of ElasTest
	*	@ return boolean
	*/
	def startElastest(){
		echo '[INI] startElastest'
		def start_elastest_result = 1
		
		//prepare mem
		
		this.@ctx.sh "sudo sysctl -w vm.max_map_count=262144"
		
		def public_ip = this.@ctx.sh (
											script: 'curl -s ipinfo.io/ip',
											returnStdout: true
										).trim()
		if ( this.@is_Authenticated ){
			//create password 
			this.@elastest_pass = "elastest_"+ this.@ctx.env.BUILD_ID+ this.@ctx.env.BUILD_NUMBER
			
			
			// def elastests_options = ' start --pullcore --user='+this.@elastest_user+ ' --password='+this.@elastest_pass+' --server-address='+public_ip+' '+this.@mode
			def elastests_options = ' start --pullcore --user='+this.@elastest_user+ ' --password='+this.@elastest_pass+' '+this.@mode
			echo elastests_options
			
			start_elastest_result = this.@ctx.sh script: ""+elastest_docker_start + this.@version+ elastests_options,				  
												 returnStatus:true
		}
		else {
			//def elastests_options = ' start --pullcore --server-address='+public_ip+' '+this.@mode
			def elastests_options =  ' start --pullcore  '+this.@mode
			echo elastests_options

			start_elastest_result = this.@ctx.sh script: ""+elastest_docker_start + this.@version+ elastests_options,				
													returnStatus:true
		}
		
		echo 'startElastest-- start_elastest_result = '+start_elastest_result
		
		if ( this.@with_ere ){
			echo 'pulling ere'
			pullERE( ere_version )
			
		}
		echo '[END] startElastest'
		return (start_elastest_result==0)
	}
	
	/*
	* Checks the availability of the Remote shared ElasTest
	*/
	def testRemoteElastest(){
		try {  
			echo '[INI] testRemoteElastest'		
			def elastest_url =  'http://'+this.@ip+':'+this.@port

			echo ' '+elastest_url
			
			def response = this.@ctx.httpRequest  authentication: 'nightly_elastest',
												  consoleLogResponseBody: true,  
												  ignoreSslErrors: true,
												  url: elastest_url
										
			echo '[END] testRemoteElastest'
			return 0
			
		} catch (Exception e) {	
			echo '[END] FAILED: testRemoteElastest '+e
			return 1
		}
	}
		
	/*
	*	Check if ElasTest exclusive local instance is running (platform and etm)
	*	@return boolean
	*/
	def elastestIsRunning(){
		echo '[INI] elastestIsRunning'
		
		def platform_state = this.@ctx.sh script: 'docker ps | grep elastest_platform | grep -c Up', returnStatus:true
		def etm_state = this.@ctx.sh script: ' docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' inspect --api ', returnStatus:true		
		
		echo '[END] elastestIsRunning : platform_state:'+platform_state+' etm_state:'+etm_state
		
		return (platform_state ==0 && etm_state==0)

	}
	/*
	*	Check if ElasTest exclusive local instance is running (platform and etm)
	*	@return boolean
	*/
	def elastestIsStuck(){
		echo '[INI] elastestIsStuck'
		
		def platform_state = this.@ctx.sh script: 'docker ps | grep elastest_platform | grep -c Up', returnStatus:true
		def etm_state = this.@ctx.sh script: ' docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' inspect --api ', returnStatus:true		
		
		echo '[END] elastestIsStuck : platform_state:'+platform_state+' etm_state:'+etm_state
		
		return (platform_state !=0 || etm_state!=0)

	}
	
	/*
	*	Wait for Local exclusive Elastest to run (unconfigurable, just the provided wait in the toolbox)
	*	@return boolean
	*/
	def waitElastest(){
		echo '[INI] waitElastest'
		def boolean elastest_is_running = false
		def counter = 3
		
		while (!elastest_is_running && counter > 0){
			def running =  this.@ctx.sh script: 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' wait --container=900', returnStatus:true
			def return_status = this.@ctx.sh script: 'docker ps | grep elastest_', returnStatus:true
			elastest_is_running = (return_status == 0 ) && (running == 0);
			echo '['+counter+']elastest_is_running:'+elastest_is_running
			counter = counter -1
		}
		echo '[END] waitElastest elastest_is_running = '+elastest_is_running
		return elastest_is_running 
	}
	
	/*
	*	Initialize the interface variables (ip, port, user and password) for both shared and exclusive instances
	*	in order to be retrieved by the pipeline
	*/
	def initializeApi(){
		echo '[INI] getAPI'
		if ( ! this.@shared == true ){
			def get_api= this.@ctx.sh (
					script: 'docker inspect --format=\\"{{.NetworkSettings.Networks.elastest_elastest.Gateway}}\\" elastest_etm-proxy_1',
					returnStdout: true
				).trim()
			echo "ETM container IP=${get_api}"

			this.@ip = get_api
			this.@ip = this.@ip.replaceAll('"','')
			echo '[END] getAPI  ip:port ' +this.@ip+':'+this.@port
		}
		else {
			this.@ip= this.@ctx.sh (
				script: 'echo $SHARED_ELASTEST_IP',
				returnStdout: true
			).trim()
			this.@elastest_user= this.@ctx.sh (
				script: 'echo $SHARED_ELASTEST_USER',
				returnStdout: true
			).trim()
			this.@elastest_pass= this.@ctx.sh (
				script: 'echo $SHARED_ELASTEST_PASS',
				returnStdout: true
			).trim()
		}
		echo '[END] getAPI  ip:port ' +this.@ip+':'+this.@port
	}
	
	/*
	*	StopElastest if it's running. 
	*	In case the instance used is the shared one it does nothing
	*/
	def stopElastest(){
		echo '[INI] stopElastest'
		if (! this.@shared == true){
			def stop_elastest_result = this.@ctx.sh script:'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' stop', returnStatus:true
			def stop_containers_result = this.@ctx.sh script:"docker ps -a -f name=elastest -q |xargs docker kill", returnStatus:true
			def delete_images_result = 	 this.@ctx.sh script:"docker images -q |xargs docker rmi -f ", returnStatus:true
			def delete_volumes_result = this.@ctx.sh script: "docker volume ls |xargs docker volume rm -f", returnStatus:true											
			echo 'stop_elastest_result = '+(stop_elastest_result &&	stop_containers_result && stop_containers_result && delete_images_result && delete_volumes_result)
		}		
		echo '[END] stopElastest'
	}
	
	
	
	/*ElasTest Properties configuration*/
	
	/*
	*	Initialization of the version parameter for the ElasTest
	*/
	def setVersion( String value) { this.@version = value }
	
	/*
	*	Initialization of the parameter mode just for personalization for normal execution leave empty
	*	for also accepts: full experimental and experimental-lite execution
	*/
	def setMode( String value) { 
		if (value != null){
			if (value=="experimental") this.@mode=experimental
			else if (value=="experimental-lite") this.@mode=experimental_lite
			else this.@mode=""
		}
		else this.@mode=""	
	}
	
	/*
	*	Authenticated elasTest by default it will use an authenticated instance. 
	*	For backward compatibility you can override it by setting it to false.
	*/
	def setAuthenticatedElastest( boolean value) { this.@is_Authenticated = value }
	
	
	/*Library Initialization*/
	
	/*
	*	Initialization of the verbose parameter for the ElasTest
	*/
	def setVerbose( boolean value) { this.@verbose = value }
	
	/*
	*	Initialization of the context. It is mandatory for the correct usage of the library
	*/
	def setContext(value){ this.@ctx = value }
	
	/*
	*	Initialization of the shared ElasTest for multiple jobs
	*/
	def setShared( boolean value) { this.@shared = value }
	
	def setEre (String value){ this.@ere_version = value
							   this.@with_ere = true }
	/*
	*	Override for echo as if not accessed in the context doesn't work
	*/
	def echo (String str){
		if (verbose ){
			this.@ctx.sh 'echo '+str
		}
		//else doesnt write nothing...  
			
	}
	
}
