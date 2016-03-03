sequential: SequentialServer.java WebRequestHandler.java ConfigParser.java SHTTPTestClient.java
	javac *.java

test-sequential: sequential
	java SequentialServer -config config.conf

test-client: sequential
	java SHTTPTestClient -server localhost -servname test -port 6789 -parallel 100 -files filelist -T 1
