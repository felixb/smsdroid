package de.ub0r.android.smsdroid;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import de.ub0r.android.lib.apis.TelephonyWrapper;

public final class MyTextWatcher implements TextWatcher {
	/** {@link TelephonyWrapper}. */
	public static final TelephonyWrapper TWRAPPER = TelephonyWrapper.getInstance();

	/** Minimum length for showing sms length. */
	private static final int TEXT_LABLE_MIN_LEN = 50;

	private final Context context;
	private final ClipboardManager cbmgr;
	private final TextView tvTextLabel;
	private final TextView tvPaste;

	/**
	 * Constructor.
	 * 
	 * @param ctx
	 *            {@link Context}
	 * @param paste
	 *            {@link TextView} holding "paste" button
	 * @param label
	 *            {@link TextView} holding message counter
	 */
	public MyTextWatcher(final Context ctx, final TextView paste, final TextView label) {
		this.context = ctx;
		this.tvTextLabel = label;
		this.tvPaste = paste;
		this.cbmgr = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterTextChanged(final Editable s) {
		final int len = s.length();
		if (len == 0) {
			if (this.cbmgr.hasText()
					&& !PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean(
							PreferencesActivity.PREFS_HIDE_PASTE, false)) {
				this.tvPaste.setVisibility(View.VISIBLE);
			} else {
				this.tvPaste.setVisibility(View.GONE);
			}
			this.tvTextLabel.setVisibility(View.GONE);
		} else {
			this.tvPaste.setVisibility(View.GONE);
			if (len > TEXT_LABLE_MIN_LEN) {
				this.tvTextLabel.setVisibility(View.VISIBLE);
				int[] l = TWRAPPER.calculateLength(s.toString(), false);
				this.tvTextLabel.setText(l[0] + "/" + l[2]);
			} else {
				this.tvTextLabel.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void beforeTextChanged(final CharSequence s, final int start, final int count,
			final int after) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void onTextChanged(final CharSequence s, final int start, final int before,
			final int count) {
	}
}
