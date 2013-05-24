package org.jboss.tusk.smartdata.data;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;

/**
 * Container for NAT log messages to be stored in the data grid.
 * @author justin
 *
 */
@Indexed @ProvidedId
public abstract class CachedItem implements Serializable {

	private static final long serialVersionUID = -4316806031289018933L;
	private static final Random rand = new Random();
	
	public abstract String getKey();
	public abstract void setKey(String key);

	public abstract String toString();
	
	public abstract CachedItem sample();

	/**
	 * Return a random sequence of 32 ints. It cannot start with 0.
	 * @return
	 */
	public static String makeKey() {
		StringBuffer buf = new StringBuffer(32);
		
		Random r = new Random();
		for (int i = 0; i < 32; i++) {
			buf.append(r.nextInt(10));
		}
		if (buf.charAt(0) == '0') {
			buf.setCharAt(0, '1');
		}
		
		return buf.toString();
	}
	
	public static String makeKeyFromUUID() {
		String key = UUID.randomUUID().toString().toLowerCase();
		key = key.replaceAll("-", "").replaceAll("a", "0").replaceAll("b", "1").
				replaceAll("c", "2").replaceAll("d", "3").replaceAll("e", "4").replaceAll("f", "5");

		//replace the leading 0 with a 1 so it's valid JSON
		if (key.startsWith("0")) {
			key = key.replaceFirst("0", "1");
		}
		
		return key;
	}
	
	public static String makeRandomString() {
		return makeRandomString(8);
	}
	
	public static String makeRandomString(int len) {
		return UUID.randomUUID().toString().replaceAll("-", "").substring(0, Math.min(len, 32));
	}
	
	public static String makeRandomIP() {
		StringBuilder builder = new StringBuilder();
		builder.append("192.168.").append(new Random().nextInt(10)).append(".").append(new Random().nextInt(256));
		return builder.toString();
	}
	
	public static String makeRandomMAC() {
		//00:0A:73:B3:15:23
		//         B3:15:23
		String alphabet = "0123456789ABCDEF";

		StringBuilder builder = new StringBuilder();
		builder.append("00:")
		.append("0A:")
		.append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(":")
		.append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(":")
		.append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(":")
		.append(alphabet.charAt(rand.nextInt(alphabet.length()))).append(alphabet.charAt(rand.nextInt(alphabet.length())));
		
		return builder.toString();
	}
	
	public int makeRandomInt(int max) {
		return rand.nextInt(max);
	}
	
	public long makeRandomLong() {
		return rand.nextLong();
	}
	
	public abstract Object getValForField(String fieldName);
	
	protected static int ignoreUntilChar(StringBuffer buf, char c, int i) {
		while (buf.charAt(i) != c) {
			i++;
		}
		return i;
	}

	protected static int ignoreUntilNextItem(StringBuffer buf, int i) {
		while (!Character.isWhitespace(buf.charAt(i)) && buf.charAt(i) != ']' && buf.charAt(i) != '}') {
			i++;
		}
		return i;
	}
	
	protected static int ignoreWhitespace(StringBuffer buf, int i) {
		while (buf.charAt(i) == ' ' || buf.charAt(i) == '\n' || buf.charAt(i) == '\t' || i > buf.length()) {
			i += 1;
		}
		return i;
	}
	
	protected static boolean keepGoing(StringBuffer buf, int i, String field) {
//		System.out.println("  in keepGoing for " + field + " and starting index " + i);// + ": buf is " + buf.substring(i));
		
		//stop if curr char is :
		boolean gotFieldSoFar = buf.charAt(i) == ':';
		if (!gotFieldSoFar) {
//			System.out.println("    failed at :");
			return true;
		}

		//and next char back is "
		gotFieldSoFar = gotFieldSoFar && (buf.charAt(i-1) == '\"');
		if (!gotFieldSoFar) {
//			System.out.println("    failed at first \"");
			return true;
		}
		
		//and next field.length() chars back are equal to field reversed
		for (int j = 0; j < field.length(); j++) {
//			System.out.println("      *****");
			gotFieldSoFar = (gotFieldSoFar && (buf.charAt(i-2-j) == field.charAt(field.length() - j - 1)));
//			System.out.println("      &&&&& idx=" + (i-2-j) + ", char=" + buf.charAt(i-2-j) + ", result=" + gotFieldSoFar);
			if (!gotFieldSoFar) {
//				System.out.println("    failed at index " + j + " of " + field + " which is checking " + (i-2-j) + " and got char " + field.charAt(field.length() - j));
				return true;
			}
		}
		
		//and next char back is "
		gotFieldSoFar = (gotFieldSoFar && (buf.charAt(i-2-field.length()) == '\"'));
//		System.out.println("    after final \" we are returning " + !gotFieldSoFar + "; total str looked at is " + buf.substring((i-2-field.length()), i));

		return !gotFieldSoFar;
	}
	
	protected static ParserTuple getJSONField(StringBuffer buf, String field, boolean useQuotes, int i) {
		return getJSONField(buf, field, useQuotes, false, i);
	}

	protected static ParserTuple getJSONField(StringBuffer buf, String field, boolean useQuotes, boolean isEndOfListValue, int i) {
		int end = i;
		
//		System.out.println("Getting JSON field for " + field + " starting at " + i + "; which is " + buf.charAt(Math.max(i, 0)));
		
		//ignore until after fieldName + ":"
		do {
			i++;
		} while (keepGoing(buf, i, field));
//		System.out.println("After skipping until field we have i=" + i + "; which is " + buf.charAt(i));
		
		//ignore whitespace starting after the current char, and the " if we are using quotes (so until the : or :\")
		i = ignoreWhitespace(buf, i + 1);
//		System.out.println("After ignoring whitespace we have i=" + i + "; which is " + buf.charAt(i));
		
		//skip over the " if we are using quotes
		i += (!useQuotes ? 0 : 1);
		
		//get index of the end of this field's value, which will be a comma (,) if we are not using quotes, or a " if we are
		if (!isEndOfListValue) {
			//char terminating the value is a normal char
			end = ignoreUntilChar(buf, (useQuotes ? '\"': ','), i);
		} else {
			//char terminating the value is any whitespace
			end = ignoreUntilNextItem(buf, i);
		}
//		System.out.println("After finding end of field val we have end=" + end + "; which is " + buf.charAt(end));
		
		//get the value
//		System.out.println("Getting value from " + i + " until " + end);
		String value = buf.substring(i, end);
		
		//move index past the value we just parsed, ignoring the final " if we are using quotes 
		i = end + (!useQuotes ? 1 : 2);
		
		return new ParserTuple(value, i);
	}
}

