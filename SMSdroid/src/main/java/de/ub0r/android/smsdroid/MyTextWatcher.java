package de.ub0r.android.smsdroid;

import android.content.Context;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import de.ub0r.android.logg0r.Log;

public final class MyTextWatcher implements TextWatcher {

    private static final String TAG = "TextWatcher";

    /**
     * Minimum length for showing sms length.
     */
    private static final int TEXT_LABLE_MIN_LEN = 50;

    private final Context context;

    private final ClipboardManager cbmgr;

    private final TextView tvTextLabel;

    private final TextView tvPaste;

    /**
     * Constructor.
     *
     * @param ctx   {@link Context}
     * @param paste {@link TextView} holding "paste" button
     * @param label {@link TextView} holding message counter
     */
    public MyTextWatcher(final Context ctx, final TextView paste, final TextView label) {
        context = ctx;
        tvTextLabel = label;
        tvPaste = paste;
        cbmgr = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    public void afterTextChanged(final Editable s) {
        final int len = s.length();
        if (len == 0) {
            if (cbmgr.hasText()
                    && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                    PreferencesActivity.PREFS_HIDE_PASTE, false)) {
                tvPaste.setVisibility(View.VISIBLE);
            } else {
                tvPaste.setVisibility(View.GONE);
            }
            tvTextLabel.setVisibility(View.GONE);
        } else {
            tvPaste.setVisibility(View.GONE);
            if (len > TEXT_LABLE_MIN_LEN) {
                try {
                    int[] l = SmsMessage.calculateLength(s.toString(), false);
                    tvTextLabel.setText(l[0] + "/" + l[2]);
                    tvTextLabel.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(TAG, "error calculating message length", e);
                }
            } else {
                tvTextLabel.setVisibility(View.GONE);
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
