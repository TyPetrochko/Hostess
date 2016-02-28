sequential: SequentialServer.java WebRequestHandler.java ConfigParser.java
	javac *.java

test: sequential
	java SequentialServer -config config.conf

