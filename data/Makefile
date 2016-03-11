all: java7 async server client

java7: Java7AsyncServer.java AsyncWebRequestHandler.java ConfigParser.java VirtualHost.java WebRequestHandler.java ILoadBalancer.java MyLoadBalancer.java FileCache.java LoadBalancer.java Debug.java
	javac $^

async: AsyncServer.java Acceptor.java AsyncWebRequestHandler.java ConfigParser.java Debug.java Dispatcher.java IAcceptHandler.java IChannelHandler.java IReadWriteHandler.java ISocketReadWriteHandlerFactory.java ReadWriteHandler.java ReadWriteHandlerFactory.java TimeoutThread.java WebRequestHandler.java ILoadBalancer.java MyLoadBalancer.java FileCache.java LoadBalancer.java
	javac $^

server: WebServer.java WebRequestHandler.java SingleThreadRequestHandler.java ConfigParser.java VirtualHost.java CompetingServiceThread.java BusyWaitServiceThread.java SuspensionServiceThread.java ILoadBalancer.java MyLoadBalancer.java LoadBalancer.java Debug.java
	javac $^

client: SHTTPTestClient.java
	javac $^

