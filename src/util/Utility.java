package util;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import model.TurboLabel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.egit.github.core.Comment;

import com.google.common.base.Joiner;

public class Utility {

	private static final Logger logger = LogManager.getLogger(Utility.class.getName());

	public static String stringify(Collection<TurboLabel> labels) {
		return "[" + Joiner.on(", ").join(labels.stream().map(TurboLabel::logString).collect(Collectors.toList())) + "]";
	}

	public static String stripQuotes(String s) {
		return s.replaceAll("^\"|\"$", "");
	}

	public static int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}

	public static Date parseHTTPLastModifiedDate(String dateString) {
		assert dateString != null;
		try {
			return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").parse(dateString);
		} catch (ParseException e) {
			assert false : "Error in date format string!";
		}
		// Should not happen
		return null;
	}

	public static String formatDateISO8601(Date date){
		assert date != null;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(date);
	}

	public static Date localDateTimeToDate(LocalDateTime time) {
		assert time != null;
		return new Date(localDateTimeToLong(time));
	}

	public static LocalDateTime dateToLocalDateTime(Date date) {
		assert date != null;
		return longToLocalDateTime(date.getTime());
	}

	public static long localDateTimeToLong(LocalDateTime t) {
		assert t != null;
		return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	public static LocalDateTime longToLocalDateTime(long epochMilli) {
		Instant instant = new Date(epochMilli).toInstant();
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	/**
	 * Parses a version number string in the format V1.2.3.
	 * @param version version number string
	 * @return an array of 3 elements, representing the major, minor, and patch versions respectively
	 */
	public static Optional<int[]> parseVersionNumber(String version) {
		// Strip non-digits
		version = version.replaceAll("[^0-9.]+", "");
		
		String[] temp = version.split("\\.");
		try {
            int major = temp.length > 0 ? Integer.parseInt(temp[0]) : 0;
            int minor = temp.length > 1 ? Integer.parseInt(temp[1]) : 0;
            int patch = temp.length > 2 ? Integer.parseInt(temp[2]) : 0;
            return Optional.of(new int[] {major, minor, patch});
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static String version(int major, int minor, int patch) {
		return String.format("V%d.%d.%d", major, minor, patch);
	}
	
	public static String snakeCaseToCamelCase(String str) {
        Pattern p = Pattern.compile("(^|_)([a-z])" );
        Matcher m = p.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(2).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
	}
	
	public static Rectangle getScreenDimensions() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		return new Rectangle((int) screenSize.getWidth(), (int) screenSize.getHeight());
	}
	
	public static Optional<Rectangle> getUsableScreenDimensions() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			return Optional.of(ge.getMaximumWindowBounds());
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
		return Optional.empty();
	}

}

