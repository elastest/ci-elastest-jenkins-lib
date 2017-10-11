def call(lite = ''){
	echo '[INI] startElastest'
	
	def start_elastest_result = sh script: 'docker run -d --name="elastest_platform" -v /var/run/docker.sock:/var/run/docker.sock --rm elastest/platform start --forcepull '+ lite, returnStatus:true
	echo 'startElastest-- start_elastest_result = '+start_elastest_result
	
	echo '[END] startElastest'
	return (start_elastest_result==0)
}