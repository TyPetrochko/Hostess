public class ProjectTester {

	// usage: server, server name, port, files, time
	public static void main (String [] args){
		int [] parallelSchedule = {1, 3, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200, 300};

		new SHTTPTestClient();

		System.out.printf("%10s, %10s\n", "Threads", "MB/s");

		for (int i = 0; i < 14; i ++){
			String [] argsx = {"-server", args[0], "-servname", args[1], "-port", args[2],
				"-parallel", parallelSchedule[i] + "", "-files", 
			args[3], "-T", args[4]};

			float mbps = SHTTPTestClient.configAndRun(argsx, false);

			System.out.printf("%10d %10f\n", parallelSchedule[i], mbps);
		}
	}
}