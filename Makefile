async: AsyncServer.java Acceptor.java AsyncWebRequestHandler.java ConfigParser.java Debug.java Dispatcher.java IAcceptHandler.java IChannelHandler.java IReadWriteHandler.java ISocketReadWriteHandlerFactory.java ReadWriteHandler.java ReadWriteHandlerFactory.java TimeoutThread.java
	javac $^

server: WebServer.java WebRequestHandler.java SingleThreadRequestHandler.java ConfigParser.java VirtualHost.java CompetingServiceThread.java BusyWaitServiceThread.java SuspensionServiceThread.java
	javac $^

client: SHTTPTestClient.java
	javac $^

test-sequential: server
	java WebServer sequential -config data/config.conf

test-req: server
	java WebServer per-request-thread -config data/config.conf

test-competing: server
	java WebServer competing -config data/config.conf

test-busy: server
	java WebServer busywait -config data/config.conf

test-suspension: server
	java WebServer suspension -config data/config.conf

test-client: client
	java SHTTPTestClient -server localhost -servname test -port 6789 -parallel 100 -files data/filelist -T 10

test-async: async
	java AsyncServer -config data/config.conf


clean:
	rm *.class
