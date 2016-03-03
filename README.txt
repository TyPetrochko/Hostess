This is a very simple skeleton/partial implementation of parts one and two of the
assignment. The client tester and Sequential Server are done at an MVP level, while
the other four servers are merely stubbed out. To see the Sequential Server in action
run:
	
$ java -jar SequentialServer.jar -config config.conf

... where config.conf is in the current working directory. To test it, use the 
following command to enact the client tester.

$ java -jar SHTTPTestClient.jar -server localhost -servname test -port 6789 -parallel 100 -files filelist -T 1

Of course, feel free to vary the config file, testing port, number of parallel
testing threads, the file-list, and testing time.

Note that the client does not implement statistics; rather, every thread merely
reports via stdout when it has completed downloading a file. This gives a rough
estimate for throughput.
