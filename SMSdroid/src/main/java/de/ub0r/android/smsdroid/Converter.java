/*
 * Copyright (C) 2011-2012-2012 Lado Kumsiashvili, Felix Bechstein
 * 
 * This file is part of SMSdroid.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.ub0r.android.smsdroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * Converts a string containing &#...; escapes to a string of characters. Taken
 * from http://rishida.net/tools/conversion/
 * 
 * @author lado
 */
public final class Converter {

	/** Private constructor. */
	private Converter() {
	}

	/** Pattern for NCR. */
	private static final Pattern PATTERN = Pattern.compile("&#([0-9]{1,7});");

	/**
	 * Converts a string containing &#...; escapes to a string of characters.
	 * 
	 * @param str
	 *            {@link CharSequence} to convert
	 * @return converted {@link CharSequence}
	 */
	public static CharSequence convertDecNCR2Char(final CharSequence str) {
		if (TextUtils.isEmpty(str)) {
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
	 * Converts a single string representing a decimal number to a character
	 * note that no checking is performed to ensure that this is just a hex
	 * number, eg. no spaces etc dec: string, the dec codepoint to be converted.
	 * 
	 * @param str
	 *            single {@link String}
	 * @return decoded {@link String}
	 */
	private static String dec2char(final String str) {
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
