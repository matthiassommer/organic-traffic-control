package de.dfg.oc.otc.manager;

import java.io.PrintStream;
import java.io.StringWriter;

/**
 * Created by Dietmar on 08.05.2017.
 * This class provides functionality to make System.out.println() work with the Aimsun console!
 */
public class OutPrintLn {

    /**
     * Use a StringWriter to store
     */
    private static StringWriter sw = new StringWriter();

    /**
     * Overrides the stream in System.out
     */
    private static PrintStream newStream = new PrintStream(System.out) {
        @Override
        public void println(String s) {
            super.println(s);
            sw.append(s);
            sw.append(System.getProperty("line.separator"));
        }
    };

    static
    {
        System.setOut(newStream);
    }

    /**
     * This method is called by the C++ API via OTCManager
     * @return Returns the string content of the out-stream and flushes it
     */
    public static String getPrintStreamOutput()
    {
        String str = sw.toString();
        sw.getBuffer().setLength(0);
        return str;
    }
}
