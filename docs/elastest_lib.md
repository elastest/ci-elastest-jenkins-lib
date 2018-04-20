[![][ElasTest Logo]][ElasTest]

Copyright © 2017-2019 [ElasTest]. Licensed under [Apache 2.0 License].

ci-elastest-jenkins-lib
==============================

ElasTest Jenkins Library (for pipelines).

Documentation
-----------------

The library is provided for managing an elastest platform inside a Jenkins CI. This library will take care of all the 
setup, launch, and stop ElasTest leaving the job free to just manage its own stages.


Usage
-----------------

First of all the job should declare the library and initialize the main configuration of the library:

```
@Library('ciElastestConnect') _

// initialization of the context for the library
elastest_lib.setContext(this)

// initialization of the runnable object to access elastest methods inside the pipeline
def elastest= elastest_lib.getElastestMethods()

```

Then we should declare configuration options: shared, mode, version, ere, authentication... (see Configuration Options)


And then you should declare all the stages of the pipeline inside the `elastest.pipeline({...})`:
```
elastest_lib.pipeline({
	stage "stage 1"
		...
		
	stage "E2E tests"
		...
})
```

Configuration options
-----------------------
The following properties can be configured in order to grant a more accurate ElasTest platform
* 	__shared__ used only for administration testing jobs. 
	*	Default value: _false_
	* Setter: _elastest_lib.setShared(true/false)_
*	__mode__ the elastest mode
	* Default value: _""_  (normal)
	* Setter: _elastest_lib.setMode("experimental"/"experimental-lite"/"")_ 
*	__version__ is the defined version of the elastest that should be used.
	* Default value: _"latest"_
	* Setter: _elastest_lib.setVersion("20171017")_ 
*	__ERE__: for selecting version of the ERE to use.
	* Default value: _false_ (no ere will be pulled)
	* Setter: _elastest_lib.setEre("latest")_
*	__authentication__: request for an authenticated ElasTest. It is ignored when used on a remote ElasTest
	* Default value: _false_
	* Setter: _elastest_lib.setAuthenticatedElastest(true/false)_
*	__testLink__: for launching ElasTest with the testLink configuration.
	* Default value: _false_
	* Setter: _elastest_lib.setTestLink(true/false)_
*	__verbose__: it configures the library and the elastest platform to run in the verbose mode, where most of the logs will be printed (library) or accesible (platform)

API
----------------

The library provides some methods to interact with the ElasTest platform:
* `elastest.getIP()` returns the ip of the ElasTest platform
* `elastest.getPort()` returns the port of the Elastest platform
* `elastest.getEtmUrl()` returns the connection chain for the etm.
* `elastest.connect2ElastestNetwork()` connects the running container with the elastest network. It is necessary for e2e tests running inside a custom container.
* `elastest.getElasTestUser()` returns the user for authenticate into the ElasTest
* `elastest.getElasTestPassword()` returns the password for authenticate into the ElasTest.


Important !!!
----------------
1. Some instructions and plugins can't be executed in a straight forward way because the context of the pipeline
is unreferenced in the library. 

These instructions throws errors like the following:
```
def mycontainer = docker.image('...')

java.lang.NullPointerException: Cannot invoke method image() on null object
	at org.codehaus.groovy.runtime.NullObject.invokeMethod(NullObject.java:91)
	at org.codehaus.groovy.runtime.callsite.PogoMetaClassSite.call(PogoMetaClassSite.java:48)
	at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:48)
	at org.codehaus.groovy.runtime.callsite.NullCallSite.call(NullCallSite.java:35)
	at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:48)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:113)
```

This errors can be solved by referencing the appropriate context in the declaration:
```
def mycontainer = elastest.ctx.docker.image('...')
```

2. In case that the tests executes inside a custom container (such as [elastest/ci-docker-e2e:latest](https://hub.docker.com/r/elastest/ci-docker-e2e/)) in order to grant connectivity with the elasTest `elastest.connect2ElastestNetwork()` should be called before the tests execution.


[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[ElasTest]: http://elastest.io/
[ElasTest Logo]: http://elastest.io/images/logos_elastest/elastest-logo-gray-small.png
[ElasTest Twitter]: https://twitter.com/elastestio
[GitHub ElasTest Group]: https://github.com/elastest
