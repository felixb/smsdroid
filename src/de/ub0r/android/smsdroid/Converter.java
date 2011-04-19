package de.ub0r.android.smsdroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author lado
 * 
 *         converts a string containing &#...; escapes to a string of characters
 * 
 *         Taken from http://rishida.net/tools/conversion/
 */
public class Converter {

	public static final Pattern PATTERN = Pattern.compile("&#([0-9]{1,7});");

	/**
	 * converts a string containing &#...; escapes to a string of characters
	 * 
	 * @param str
	 *            CharSequence to convert
	 * @return
	 */
	public static CharSequence convertDecNCR2Char(final CharSequence str) {

		if (str == null) {
			return str;
		}

		Matcher m = PATTERN.matcher(str);

		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String c = m.group();
			m.appendReplacement(sb, dec2char(c.substring(2, c.length() - 1)));

		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * converts a single string representing a decimal number to a character
	 * note that no checking is performed to ensure that this is just a hex
	 * number, eg. no spaces etc dec: string, the dec codepoint to be converted
	 * 
	 * @param str
	 * @return
	 */
	private static final String dec2char(final String str) {
		try {
			int n = Integer.valueOf(str);
			if (n <= 0xFFFF) {
				return String.valueOf((char) n);
			} else if (n <= 0x10FFFF) {
				n -= 0x10000;
				return String.valueOf((char) (0xD800 | (n >> 10)))
						+ String.valueOf((char) (0xDC00 | (n & 0x3FF)));
			}
		} catch (NumberFormatException nfe) {
			return str;
		}
		return str;
	}
}
