public class Debug {

    public static boolean DEBUG = false;
    public static void DEBUG(String s) {
	if (DEBUG)
	    System.out.println(s);
    }
}
