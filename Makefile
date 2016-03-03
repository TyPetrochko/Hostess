server: WebServer.java WebRequestHandler.java SingleThreadRequestHandler.java ConfigParser.java VirtualHost.java CompetingServiceThread.java
	javac $^

client: SHTTPTestClient.java
	javac $^

test-sequential: server
	java WebServer sequential -config config.conf

test-req: server
	java WebServer per-request-thread -config config.conf

test-competing: server
	java WebServer competing -config config.conf

test-client: client
	java SHTTPTestClient -server localhost -servname test -port 6789 -parallel 100 -files filelist -T 5

clean:
	rm *.class
