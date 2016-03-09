
java7: Java7AsyncServer.java AsyncWebRequestHandler.java ConfigParser.java VirtualHost.java WebRequestHandler.java ILoadBalancer.java MyLoadBalancer.java FileCache.java LoadBalancer.java
	javac $^

async: AsyncServer.java Acceptor.java AsyncWebRequestHandler.java ConfigParser.java Debug.java Dispatcher.java IAcceptHandler.java IChannelHandler.java IReadWriteHandler.java ISocketReadWriteHandlerFactory.java ReadWriteHandler.java ReadWriteHandlerFactory.java TimeoutThread.java WebRequestHandler.java ILoadBalancer.java MyLoadBalancer.java FileCache.java LoadBalancer.java
	javac $^

server: WebServer.java WebRequestHandler.java SingleThreadRequestHandler.java ConfigParser.java VirtualHost.java CompetingServiceThread.java BusyWaitServiceThread.java SuspensionServiceThread.java ILoadBalancer.java MyLoadBalancer.java LoadBalancer.java
	javac $^

client: SHTTPTestClient.java
	javac $^

test-sequential: server
	java -Xprof WebServer sequential -config data/config.conf

test-req: server
	java WebServer per-request-thread -config data/config.conf

test-competing: server
	java WebServer competing -config data/config.conf

test-busy: server
	java WebServer busywait -config data/config.conf

test-suspension: server
	java WebServer suspension -config data/config.conf

test-client: client
	java SHTTPTestClient -server peacock.zoo.cs.yale.edu -servname test -port 6789 -parallel 100 -files data/filelist -T 10

test-local: client
	java SHTTPTestClient -server localhost -servname test -port 6789 -parallel 100 -files data/filelist -T 10

test-lee: client
	java SHTTPTestClient -server peacock.zoo.cs.yale.edu -servname peacock.zoo.cs.yale.edu -port 6755 -parallel 10 -files data/filelist -T 5

test-alan: client
	java SHTTPTestClient -server 128.36.232.18 -servname zoo -port 3000 -parallel 30 -files data/filelist -T 10

test-async: 
	java AsyncServer -config data/config.conf

test-java7: 
	java Java7AsyncServer -config data/config.conf

test-apache:
	java SHTTPTestClient -server zoo.cs.yale.edu -servname zoo -port 80 -parallel 5 -files data/list1 -T 10

load-balancer: ILoadBalancer.java MyLoadBalancer.java
	javac $^

clean:
	rm *.class
