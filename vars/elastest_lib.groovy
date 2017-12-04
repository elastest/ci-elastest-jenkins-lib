//With classes
class elastest_lib implements Serializable {
	//configuration of the library
	private boolean verbose = false
	
	private experimental = "--mode=experimental"
	private experimental_lite = "--mode=experimental-lite"
	//info of the ElasTest
	private String ip
	private String port="37000"
	
	private String sharedElastest_ip = ""
	
	//parameters for the ElasTest
	private boolean shared = false
	private String mode = '' //default normal
	private String version='latest'
	
	def ctx
	
	
	/*
	*	Initialization of the verbose parameter for the ElasTest
	*/
	def setVerbose( boolean value) { this.@verbose = value }
	
	/*
	*	Initialization of the version parameter for the ElasTest
	*/
	def setVersion( String value) { this.@version = value }
	
	/*
	*	Initialization of the context. It is mandatory for the correct usage of the library
	*/
	def setContext(value){ this.@ctx = value }
	
	/*
	*	Initialization of the parameter Lite just for personalization for normal execution leave empty
	*	for lite execution initialize with '--lite'
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
	*	Initialization of the shared ElasTest for multiple jobs
	*/
	def setShared( boolean value) { this.@shared = value }
	
	
	/*
	*	@return elastest_ip
	*/
	def getIp() { 
		if (this.@shared == true )
			return sharedElastest_ip
	return this.@ip }

	/*
	*	@return elastest_port
	*/
	def getPort() { return this.@port }
	
	/*
	*	@ return full conection url for torm
	*/
	def getEtmUrl() { 
		if (this.@shared == true )
				return 'http://'+sharedElastest_ip+':'+this.@port  
		return 'http://'+this.@ip+':'+this.@port 
	}
	
	/*
	*	When using methods of this library inside any provided step should be called through the 
	*	returned object by this method
	*	@return this object (executable library)
	*/
	def getElastestMethods(){ return this }
	
	
	/*
	* checks availability of remote elastest
	*/
	def testRemoteElastest(){
		try {  
				echo '[INI] testRemoteElastest'		
				echo 'http://'+sharedElastest_ip+':'+this.@port		
				new URL('http://'+sharedElastest_ip+':'+this.@port).getText()
				
				echo '[END] testRemoteElastest'
				return 0
				
			} catch (Exception e) {
				
				echo '[END] FAILED: testRemoteElastest '+e
				return 1
			}
		}
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
	
	
	/*
	*	Start elastest if it is not running
	*	@return boolean
	*/
	def startElastest(){
		echo '[INI] startElastest'
		def start_elastest_result = 1
		if (! this.@shared){
			//prepare mem
			this.@ctx.sh "sudo sysctl -w vm.max_map_count=262144"
			
			start_elastest_result = this.@ctx.sh script: 'docker run -d --name="elastest_platform" -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform:'+this.@version+' start --pullcore '+ this.@mode, returnStatus:true
			echo 'startElastest-- start_elastest_result = '+start_elastest_result
		}
		else {
			start_elastest_result = testRemoteElastest()
		}
		echo '[END] startElastest'
		return (start_elastest_result==0)
	}
	
	/*
	*	Check if ElasTest is running (platform and etm)
	*	@return boolean
	*/
	def elastestIsRunning(){
		echo '[INI] elastestIsRunning'
		def elastest_state = false
		
		if (this.@shared == true ){
			def platform_state = this.@ctx.sh script: 'docker ps | grep elastest_platform | grep -c Up', returnStatus:true
			def etm_state = this.@ctx.sh script: ' docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' wait --running=0 ', returnStatus:true
			elastest_state = (platform_state==0 && etm_state==0)
		}
		else {
			def remoteElastest = testRemoteElastest()
			elastest_state = (remoteElastest==0)
		}
		return elastest_state
		echo '[END] elastestIsRunning'
	}

	/*
	*	Wait for Elastest to run (unconfigurable, just the provided wait in the toolbox)
	*	@return boolean
	*/
	def waitElastest(){
		echo '[INI] elastestIsRunning'
		def elastest_is_running = false
		
		if (this.@shared == true ){
			elastest_is_running = this.@ctx.sh script: 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' wait --container=900', returnStatus:true
		}
		else {
			elastest_is_running = testRemoteElastest()
		}
		
		return (elastest_is_running == 0)
	}

	/*
	*	StopElastest if it's running
	*/
	def stopElastest(){
		echo '[INI] stopElastest'
		if (this.@shared == true ){
			def stop_elastest_result = this.@ctx.sh script:'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock elastest/platform:'+this.@version+' stop', returnStatus:true
			def stop_containers_result = this.@ctx.sh script:"docker ps -q |xargs docker rm ", returnStatus:true
			def delete_images_result = 	 this.@ctx.sh script:"docker images -q |xargs docker rmi -f ", returnStatus:true
			def delete_volumes_result = this.@ctx.sh script: "docker volume ls |xargs docker volume rm -f", returnStatus:true											
			echo 'stop_elastest_result = '+(stop_elastest_result &&	stop_containers_result && stop_containers_result && delete_images_result && delete_volumes_result)
		}		
		echo '[END] stopElastest'
	}

	
	/*
	*	Get the API
	*/
	def getApi(){
		echo '[INI] getAPI'
		if (this.@shared == true ){
			def get_api= this.@ctx.sh (
					script: 'docker inspect --format=\\"{{.NetworkSettings.Networks.elastest_elastest.Gateway}}\\" elastest_etm-proxy_1',
					returnStdout: true
				).trim()
			echo "ETM container IP=${get_api}"
			
			//this is valid for output:
			/*
			Waiting for ETM...
			Platform inserted into network succesfully

			Container created with IP: 172.18.0.19
			ETM is ready in http://172.18.0.19:8091

			ElasTest API info:
			Url: http://172.18.0.19:8091
			*/
			
			/*def strings = get_api.tokenize( ':' )
			this.@ip = strings[strings.size()-2].replaceAll("/",'')
			this.@port = strings[strings.size()-1]
			*/
			this.@ip=get_api
			this.@ip = this.@ip.replaceAll('"','')
			echo '[END] getAPI  ip:port ' +this.@ip+':'+this.@port
		}
		else {
			echo '[END] getAPI  ip:port ' +sharedElastest_ip+':'+this.@port
		}
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
					//test environment values
					this.@ctx.sh 'env > env.txt'
					this.@ctx.sh 'cat env.txt'
					
					echo "sharedElastest ="+this.@shared

					def elastest_is_running = startElastest()
					if (! elastest_is_running){
						this.@ctx.currentBuild.result = 'FAILURE'
						return
					}
					else {
							getApi()
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
					def elastest_is_running = elastestIsRunning()
					if (elastest_is_running){ //stop and start again --> elastest is unique and frethis.@ctx.sh with each start
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
							getApi()
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
	*	Override for echo as if not accessed in the context doesn't work
	*/
	def echo (String str){
		if (verbose ){
			this.@ctx.sh 'echo '+str
		}
		//else doesnt write nothing...  
			
	}
	
}




