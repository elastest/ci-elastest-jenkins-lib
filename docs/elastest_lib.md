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

If the job make use of the lite version of the ElasTest the next instruction should be added:
```
// lite version of elastest
elastest_lib.setLite(' --lite') 
```

Then all the stages of the pipeline should be added inside the `elastest.pipeline({...})`:
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
* 	*shared* used only for administration testing jobs. 
	**	Default value: _false_
	** Setter: _elastest_lib.setShared(true/false)_
*	*lite*: if the job should be executed with the lite version of elastest.
	** Default value: _""_ 
	** Setter: _elastest_lib.setLite("--lite"/"")_ 
*	*version* is the defined version of the elastest that should be used.
	** Default value: _"latest"_
	** Setter: _elastest_lib.setLite("20171017")_ 


API
----------------

The library provides some methods to interact with the ElasTest platform:
* `elastest.getIP()` returns the ip of the ElasTest platform
* `elastest.getPort()` returns the port of the Elastest platform
* `elastest.getEtmUrl()` returns the conection chain for the etm.


Important !!!
----------------
Some instructions and plugins can't be executed in a straight forward way because the context of the pipeline
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

This errors can be soved by referencing the appropriate context in the declaration:
```
def mycontainer = elastest.ctx.docker.image('...')
```



[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[ElasTest]: http://elastest.io/
[ElasTest Logo]: http://elastest.io/images/logos_elastest/elastest-logo-gray-small.png
[ElasTest Twitter]: https://twitter.com/elastestio
[GitHub ElasTest Group]: https://github.com/elastest
