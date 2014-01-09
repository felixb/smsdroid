/*
 * Copyright (C) 2010 Felix Bechstein, The Android Open Source Project
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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;

/**
 * @author flx, The Android Open Source Project
 */
/**
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public final class SmileyParser {
	/** Singleton stuff. */
	private static SmileyParser sInstance;

	/**
	 * Get the single instance.
	 * 
	 * @return the {@link SmileyParser}
	 */
	public static SmileyParser getInstance(final Context context) {
		if (sInstance == null) {
			sInstance = new SmileyParser(context);
		}
		return sInstance;
	}

	/** {@link Context}. */
	private final Context mContext;
	/** Smiley texts. */
	private final String[] mSmileyTexts;
	/** Smiley pattern. */
	private final Pattern mPattern;
	/** Map pattern to resource. */
	private final HashMap<String, Integer> mSmileyToRes;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context} to get resources from.
	 */
	private SmileyParser(final Context context) {
		mContext = context;
		mSmileyTexts = mContext.getResources().getStringArray(R.array.emoticons);
		mSmileyToRes = buildSmileyToRes();
		mPattern = buildPattern();
	}

	// NOTE: if you change anything about this array, you must make the
	// corresponding change
	// to the string arrays: default_smiley_texts and default_smiley_names in
	// res/values/arrays.xml
	/** Smiley resources keys. */
	public static final int[] DEFAULT_SMILEY_RES_IDS = { R.drawable.emo_im_angel, // 0
			R.drawable.emo_im_cool, // 1
			R.drawable.emo_im_cool, // 2
			R.drawable.emo_im_crying, // 3
			R.drawable.emo_im_crying, // 4
			R.drawable.emo_im_foot_in_mouth, // 5
			R.drawable.emo_im_happy, // 6
			R.drawable.emo_im_happy, // 7
			R.drawable.emo_im_kissing, // 8
			R.drawable.emo_im_kissing, // 9
			R.drawable.emo_im_laughing, // 10
			R.drawable.emo_im_laughing, // 11
			R.drawable.emo_im_lips_are_sealed, // 12
			R.drawable.emo_im_lips_are_sealed, // 13
			R.drawable.emo_im_lips_are_sealed, // 14
			R.drawable.emo_im_money_mouth, // 15
			R.drawable.emo_im_sad, // 16
			R.drawable.emo_im_sad, // 17
			R.drawable.emo_im_surprised, // 18
			R.drawable.emo_im_tongue_sticking_out, // 19
			R.drawable.emo_im_tongue_sticking_out, // 20
			R.drawable.emo_im_tongue_sticking_out, // 21
			R.drawable.emo_im_undecided, // 22
			R.drawable.emo_im_winking, // 23
			R.drawable.emo_im_winking, // 24
			R.drawable.emo_im_wtf, // 25
			R.drawable.emo_im_yelling, // 26
	};

	/**
	 * Builds the {@link HashMap} we use for mapping the string version of a
	 * smiley (e.g. ":-)") to a resource ID for the icon version.
	 * 
	 * @return {@link HashMap}
	 */
	private HashMap<String, Integer> buildSmileyToRes() {
		if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length) {
			// Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
			// and failed to update arrays.xml
			throw new IllegalStateException("Smiley resource ID/text mismatch");
		}

		HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(
				mSmileyTexts.length);
		for (int i = 0; i < mSmileyTexts.length; i++) {
			smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
		}

		return smileyToRes;
	}

	/**
	 * Builds the regular expression we use to find smileys in
	 * {@link #addSmileySpans}.
	 * 
	 * @return {@link Pattern}
	 */
	private Pattern buildPattern() {
		// Set the StringBuilder capacity with the assumption that the average
		// smiley is 3 characters long.
		StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		patternString.append('(');
		for (String s : mSmileyTexts) {
			patternString.append(Pattern.quote(s));
			patternString.append('|');
		}
		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString());
	}

	/**
	 * Adds ImageSpans to a CharSequence that replace textual emoticons such as
	 * :-) with a graphical version.
	 * 
	 * @param text
	 *            A CharSequence possibly containing emoticons
	 * @return A CharSequence annotated with ImageSpans covering any recognized
	 *         emoticons.
	 */
	public CharSequence addSmileySpans(final CharSequence text) {
		SpannableStringBuilder builder = new SpannableStringBuilder(text);

		Matcher matcher = mPattern.matcher(text);
		while (matcher.find()) {
			int resId = mSmileyToRes.get(matcher.group());
			builder.setSpan(new ImageSpan(mContext, resId), matcher.start(), matcher.end(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return builder;
	}
}
