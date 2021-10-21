package map;

public class ProcessTime {

    /* 
    // use clock() from C
    public static final long CLOCKS_PER_SEC = 1000000;
    public static final String libpath = "/home/cvs/csc2512f/src/map/ProcessTime.so";
    static {
	System.load(libpath);
	//System.loadLibrary(libpath);
    }
    public static native long clock();
    */


    // portable version
    public static final long CLOCKS_PER_SEC = 1000;
    public static long clock() {
	return System.currentTimeMillis();
    }
}
