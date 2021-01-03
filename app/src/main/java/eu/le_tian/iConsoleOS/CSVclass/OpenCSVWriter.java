package eu.le_tian.iConsoleOS.CSVclass;

import android.os.Environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class OpenCSVWriter {
    private static final char DEFAULT_SEPARATOR = ',';

    public static void writeLine(Writer w, List<String> values) throws IOException {
        writeLine(w, values, DEFAULT_SEPARATOR, ' ');
    }

    public static void writeLine(Writer w, List<String> values, char separators) throws IOException {
        writeLine(w, values, separators, ' ');
    }

    //https://tools.ietf.org/html/rfc4180
    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;

        //default customQuote is empty

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }

    public static void writeLinesToFile(String filename, List<List<String>> allValues, char separators, boolean appendToFile) throws IOException {
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        FileWriter writer = new FileWriter(baseDir + "/" + filename, appendToFile);
        for (List<String> values : allValues) {
            writeLine(writer, values, separators);
        }
        writer.flush();
        writer.close();
    }

}
