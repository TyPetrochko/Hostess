sequential: SequentialServer.java WebRequestHandler.java ConfigParser.java
	javac $^

req: RequestThreadServer.java WebRequestHandler.java ConfigParser.java
	javac $^

client: SHTTPTestClient.java
	javac $^

test-sequential: sequential client
	java SequentialServer -config config.conf

test-req: req client
	java RequestThreadServer -config config.conf

test-client: sequential
	java SHTTPTestClient -server localhost -servname test -port 6789 -parallel 100 -files filelist -T 1
