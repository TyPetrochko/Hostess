###### FILE STRUCTURE ######

'source.tar' contains all source coode

'WebServer.jar' is a generic server that satisfies the
design requirements of servers 1-5. See "TESTING" for
instructions on how to run.

'AsyncServer.jar' is server 6, i.e. an asynchronous
server using the select model.

'Java7AsyncServer.jar' is server 7, i.e. the asynchronous
server using Java 7's AsynchornousServerSocketChannel 
and AsynchornousSocketChannel classes.

'config.conf' is a config file that can be used to specify
runtime parameters for any of the servers.

'requests.txt' is a list of files that can be used for testing.
See section "TESTING".

'Makefile' is a makefile that can be used to compile the
servers.



###### TESTING ######

To run servers 1-5, invoke the following command from
the same director as WebServer.jar:

	$ java -jar WebServer.jar <Server> -config config.conf
	
Where <server> is one of the following: 

	sequential
	per-request-thread
	competing
	busywait
	suspension

To run server 6, invoke the following command from the
same directory as AsyncServer.jar:

	$ java -jar AsyncServer.jar -config config.conf

And likewise, for server 7,

	$ java -jar Java7AsyncServer.jar -config config.conf


*Alternatively, you can extract any one of the jars and run with 
the following commands:

	$ java WebServer <Server> -config config.conf
	$ java AsyncServer -config config.conf
	$ java Java7AsyncServer -config config.conf




###### BENCHMARKING ######

To benchmark, run the following command:

	$ java -jar SHTTPTestClient.jar -server <address> -servname <virtual host> -port <port> -parallel <number of threads> -files <files to download> -T <length of test>


With the appropriate parameters, i.e.

	$ java -jar SHTTPTestClient.jar -server peacock.zoo.cs.yale.edu -servname zoo -port 6789 -parallel 100 -files requests.txt -T 10


(this matches the parameters set in config.conf, 
assuming server is running on peacock). The files
listed in "requests.txt" are already on the zoo,
provided the virtualhost is set up correctly.

In my benchmarking report, I tested my Java 7 
asynchronous server versus apache.



###### LOAD BALANCING ######

To load balance, a load balancing algorithm must be
implemented in a new class that extends the ILoadBalancer
interface. Once this class is compiled, put in the same
directory as the main file, the load-balancing class
can be specified in config.conf, i.e.

	LoadBalancer NewLoadBalancer

The loadbalancer currently implemented performs a simple
decision tree based off of number of users to determine
if the server is willing to accept new connections.


###### COMPILING ######

To compile, extract 'source.tar' and use the Makefile

	$ make all

or simply
	
	$ make
