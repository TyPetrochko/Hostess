public class ProjectTester {

	// usage: server, server name, port, files, time
	public static void main (String [] args){
		int [] parallelSchedule = {1, 3, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200, 300, 500, 700};

		new SHTTPTestClient();

		System.out.printf("%15s %15s %15s\n", "Threads", "MB/s", "Delay (ms)");

		for (int i = 0; i < 16; i ++){
			String [] argsx = {"-server", args[0], "-servname", args[1], "-port", args[2],
				"-parallel", parallelSchedule[i] + "", "-files", 
			args[3], "-T", args[4]};

			float [] metrics = SHTTPTestClient.configAndRun(argsx, false);

			System.out.printf("%15d %15f %15f \n", parallelSchedule[i], metrics[0], metrics[1] * 1000);
			SHTTPTestClient.clearVars();
		}
	}
}