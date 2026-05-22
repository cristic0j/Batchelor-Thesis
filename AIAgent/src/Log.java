import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Log {
	
	 private static String lname = new SimpleDateFormat("yyMMddHHmmss").format(new Date())
     + "_log.txt";

	
	private static boolean printToScreen = true;
	private static boolean isLogOut = true;
	
	 protected static StringBuffer saveScreen = new StringBuffer();
	 
	    public static final void print(String s) {

	        if (printToScreen) {
	            System.out.print(s);
	        }
	  
	        if (isLogOut && Board.isTournament) {    
	            saveScreen.append(s);
	        }
	 	   
	    }
	    
	    public static void saveScreen(){
	    	
	    	if (isLOG()) {
	            try (
	                FileWriter fileout = new FileWriter(lname, true);
	                PrintWriter out = new PrintWriter(fileout);){

	                out.println(saveScreen);
	                out.flush();
	                saveScreen = new StringBuffer(saveScreen.length());
	            } catch (IOException e) {
	                println("Log Log.print Error");}}	
	    }

		public static final void println(String string) {
			print(string + "\r\n");		
		}
		
		public static final void println() {
			print("\r\n");		
		}

		public static void setLOG(boolean isLog) {
			isLogOut = isLog;
		}

		public static boolean isLOG() {
			return isLogOut;
		}

		public static void setPrintToScreen(boolean pRINT_TO_SCREEN) {
			printToScreen = pRINT_TO_SCREEN;
		}

		public static boolean isPrintToScreen() {
			return printToScreen;
		}

}
