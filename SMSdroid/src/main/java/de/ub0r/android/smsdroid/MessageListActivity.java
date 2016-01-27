/*
 * Copyright (C) 2009-2015 Felix Bechstein
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

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.logg0r.Log;

/**
 * {@link FragmentActivity} showing a single conversation.
 *
 * @author flx
 */
public class MessageListActivity extends AppCompatActivity implements OnItemClickListener,
        OnItemLongClickListener, OnClickListener, OnLongClickListener {

    private static final String TAG = "ml";

    /**
     * {@link ContactsWrapper}.
     */
    private static final ContactsWrapper WRAPPER = ContactsWrapper.getInstance();

    /**
     * Number of items.
     */
    private static final int WHICH_N = 8;

    /**
     * Index in dialog: mark view/add contact.
     */
    private static final int WHICH_VIEW_CONTACT = 0;

    /**
     * Index in dialog: mark call contact.
     */
    private static final int WHICH_CALL = 1;

    /**
     * Index in dialog: mark read/unread.
     */
    private static final int WHICH_MARK_UNREAD = 2;

    /**
     * Index in dialog: reply.
     */
    private static final int WHICH_REPLY = 3;

    /**
     * Index in dialog: forward.
     */
    private static final int WHICH_FORWARD = 4;

    /**
     * Index in dialog: copy text.
     */
    private static final int WHICH_COPY_TEXT = 5;

    /**
     * Index in dialog: view details.
     */
    private static final int WHICH_VIEW_DETAILS = 6;

    /**
     * Index in dialog: delete.
     */
    private static final int WHICH_DELETE = 7;

    /**
     * maximum number of lines in EditText
     */
    private static final int MAX_EDITTEXT_LINES = 10;

    /**
     * Package name for System's chooser.
     */
    private static String chooserPackage = null;

    /**
     * Used {@link Uri}.
     */
    private Uri uri;

    /**
     * {@link Conversation} shown.
     */
    private Conversation conv = null;

    /**
     * ORIG_URI to resolve.
     */
    static final String URI = "content://mms-sms/conversations/";

    /**
     * Dialog items shown if an item was long clicked.
     */
    private final String[] longItemClickDialog = new String[WHICH_N];

    /**
     * Marked a message unread?
     */
    private boolean markedUnread = false;

    /**
     * {@link EditText} holding text.
     */
    private EditText etText;

    /**
     * {@link ClipboardManager}.
     */
    @SuppressWarnings("deprecation")
    private ClipboardManager cbmgr;

    /**
     * Enable autosend.
     */
    private boolean enableAutosend = true;

    /**
     * Show textfield.
     */
    private boolean showTextField = true;

    /**
     * Show {@link Contact}'s photo.
     */
    private boolean showPhoto = false;

    /**
     * Default {@link Drawable} for {@link Contact}s.
     */
    private Drawable defaultContactAvatar = null;

    /**
     * {@link MenuItem} holding {@link Contact}'s picture.
     */
    private MenuItem contactItem = null;

    /**
     * True, to update {@link Contact}'s photo.
     */
    private boolean needContactUpdate = false;

    /**
     * Get {@link ListView}.
     *
     * @return {@link ListView}
     */
    private ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    /**
     * Set {@link ListAdapter} to {@link ListView}.
     *
     * @param la ListAdapter
     */
    private void setListAdapter(final ListAdapter la) {
        getListView().setAdapter(la);
    }

    private AdView mAdView;

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        enableAutosend = p.getBoolean(PreferencesActivity.PREFS_ENABLE_AUTOSEND, true);
        showTextField = enableAutosend
                || p.getBoolean(PreferencesActivity.PREFS_SHOWTEXTFIELD, true);
        showPhoto = p.getBoolean(PreferencesActivity.PREFS_CONTACT_PHOTO, true);
        final boolean hideSend = p.getBoolean(PreferencesActivity.PREFS_HIDE_SEND, false);
        setTheme(PreferencesActivity.getTheme(this));
        Utils.setLocale(this);
        setContentView(R.layout.messagelist);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d(TAG, "onCreate()");

        if (showPhoto) {
            defaultContactAvatar = getResources().getDrawable(
                    R.drawable.ic_contact_picture);
        }
        if (hideSend) {
            findViewById(R.id.send_).setVisibility(View.GONE);
        }

        cbmgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        etText = (EditText) findViewById(R.id.text);
        int flags = etText.getInputType();
        if (p.getBoolean(PreferencesActivity.PREFS_EDIT_SHORT_TEXT, true)) {
            flags |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        } else {
            flags &= ~InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        }
        etText.setInputType(flags);

        if (!showTextField) {
            findViewById(R.id.text_layout).setVisibility(View.GONE);
        }

        parseIntent(getIntent());

        final ListView list = getListView();
        list.setOnItemLongClickListener(this);
        list.setOnItemClickListener(this);
        View v = findViewById(R.id.send_);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
        findViewById(R.id.text_paste).setOnClickListener(this);
        /* TextWatcher updating char count on writing. */
        MyTextWatcher textWatcher = new MyTextWatcher(this,
                (TextView) findViewById(R.id.text_paste),
                (TextView) findViewById(R.id.text_));
        etText.addTextChangedListener(textWatcher);
        etText.setMaxLines(MAX_EDITTEXT_LINES);
        textWatcher.afterTextChanged(etText.getEditableText());

        longItemClickDialog[WHICH_MARK_UNREAD] = getString(R.string.mark_unread_);
        longItemClickDialog[WHICH_REPLY] = getString(R.string.reply);
        longItemClickDialog[WHICH_FORWARD] = getString(R.string.forward_);
        longItemClickDialog[WHICH_COPY_TEXT] = getString(R.string.copy_text_);
        longItemClickDialog[WHICH_VIEW_DETAILS] = getString(R.string.view_details_);
        longItemClickDialog[WHICH_DELETE] = getString(R.string.delete_message_);

        mAdView = (AdView) findViewById(R.id.ads);
        mAdView.setVisibility(View.GONE);
        if (!DonationHelper.hideAds(this)) {
            mAdView.loadAd(new AdRequest.Builder().build());
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                    super.onAdLoaded();
                }
            });
        }
    }

    @Override
    protected final void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        parseIntent(intent);
    }

    /**
     * Parse data pushed by {@link Intent}.
     *
     * @param intent {@link Intent}
     */
    private void parseIntent(final Intent intent) {
        Log.d(TAG, "parseIntent(", intent, ")");
        if (intent == null) {
            return;
        }
        Log.d(TAG, "got action: ", intent.getAction());
        Log.d(TAG, "got uri: ", intent.getData());

        needContactUpdate = true;

        uri = intent.getData();
        if (uri != null) {
            if (!uri.toString().startsWith(URI)) {
                uri = Uri.parse(URI + uri.getLastPathSegment());
            }
        } else {
            final long tid = intent.getLongExtra("thread_id", -1L);
            uri = Uri.parse(URI + tid);
            if (tid < 0L) {
                try {
                    startActivity(ConversationListActivity.getComposeIntent(this, null, false));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "activity not found", e);
                    Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
                }
                finish();
                return;
            }
        }

        int threadId;
        try {
            threadId = Integer.parseInt(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            Log.e(TAG, "unable to parse thread id: ", e);
            Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Conversation c;
        try {
            c = Conversation.getConversation(this, threadId, true);
            threadId = c.getThreadId(); // force a NPE :x
        } catch (NullPointerException e) {
            Log.e(TAG, "Fetched null conversation for thread ", threadId, e);
            Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        conv = c;
        final Contact contact = c.getContact();
        try {
            contact.update(this, false, true);
        } catch (NullPointerException e) {
            Log.e(TAG, "updating contact failed", e);
        }
        boolean showKeyboard = intent.getBooleanExtra("showKeyboard", false);

        Log.d(TAG, "address: ", contact.getNumber());
        Log.d(TAG, "name: ", contact.getName());
        Log.d(TAG, "displayName: ", contact.getDisplayName());
        Log.d(TAG, "showKeyboard: ", showKeyboard);

        final ListView lv = getListView();
        lv.setStackFromBottom(true);

        MessageAdapter adapter = new MessageAdapter(this, uri);
        setListAdapter(adapter);

        String displayName = contact.getDisplayName();
        setTitle(displayName);
        String number = contact.getNumber();
        if (displayName.equals(number)) {
            getSupportActionBar().setSubtitle(null);
        } else {
            getSupportActionBar().setSubtitle(number);
        }

        setContactIcon(contact);

        final String body = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(body)) {
            etText.setText(body);
            showKeyboard = true;
        }

        if (showKeyboard) {
            etText.requestFocus();
        }

        setRead();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private ImageView findMenuItemView(final int viewId) {
        ImageView view = (ImageView) findViewById(viewId);
        if (view != null) {
            return view;
        }

        if (contactItem != null) {
            return (ImageView) contactItem.getActionView().findViewById(viewId);
        }
        return null;
    }

    /**
     * Show {@link MenuItem} holding {@link Contact}'s picture.
     *
     * @param contact {@link Contact}
     */
    private void setContactIcon(final Contact contact) {
        if (contact == null) {
            Log.w(TAG, "setContactIcon(null)");
            return;
        }

        if (contactItem == null) {
            Log.w(TAG, "setContactIcon: contactItem == null");
            return;
        }

        if (!needContactUpdate) {
            Log.i(TAG, "skip setContactIcon()");
            return;
        }

        final String name = contact.getName();
        final boolean showContactItem = showPhoto && name != null;

        if (showContactItem) {
            // photo
            ImageView ivPhoto = findMenuItemView(R.id.photo);
            if (ivPhoto == null) {
                Log.w(TAG, "ivPhoto == null");
            } else {
                ivPhoto.setImageDrawable(contact.getAvatar(this, defaultContactAvatar));
                ivPhoto.setOnClickListener(WRAPPER.getQuickContact(this, ivPhoto,
                        contact.getLookUpUri(getContentResolver()), 2, null));
            }

            // presence
            ImageView ivPresence = findMenuItemView(R.id.presence);
            if (ivPresence == null) {
                Log.w(TAG, "ivPresence == null");
            } else {
                if (contact.getPresenceState() > 0) {
                    ivPresence.setImageResource(Contact.getPresenceRes(contact.getPresenceState()));
                    ivPresence.setVisibility(View.VISIBLE);
                } else {
                    ivPresence.setVisibility(View.INVISIBLE);
                }
            }
        }

        contactItem.setVisible(showContactItem);
        needContactUpdate = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void onResume() {
        super.onResume();

        mAdView.resume();
        final ListView lv = getListView();
        lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lv.setAdapter(new MessageAdapter(this, uri));
        markedUnread = false;

        final Button btn = (Button) findViewById(R.id.send_);
        if (showTextField) {
            Intent i;
            ActivityInfo ai = null;
            final PackageManager pm = getPackageManager();
            try {
                i = buildIntent(enableAutosend, false);
                if (pm != null && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                        PreferencesActivity.PREFS_SHOWTARGETAPP, true)) {
                    ai = i.resolveActivityInfo(pm, 0);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "unable to build Intent", e);
            }
            etText.setMaxLines(MAX_EDITTEXT_LINES);

            if (ai == null) {
                btn.setText(null);
                etText.setMinLines(1);
            } else {
                if (chooserPackage == null) {
                    try {
                        final ActivityInfo cai = buildIntent(enableAutosend, true)
                                .resolveActivityInfo(pm, 0);
                        if (cai != null) {
                            chooserPackage = cai.packageName;
                        }
                    } catch (NullPointerException e) {
                        Log.e(TAG, "unable to build Intent", e);
                    }
                }
                if (ai.packageName.equals(chooserPackage)) {
                    btn.setText(R.string.chooser_);
                } else {
                    Log.d(TAG, "ai.pn: ", ai.packageName);
                    btn.setText(ai.loadLabel(pm));
                }
                etText.setMinLines(3);
            }
        } else {
            btn.setText(null);
        }
    }

    @Override
    protected final void onPause() {
        mAdView.pause();
        if (!markedUnread) {
            setRead();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mAdView.destroy();
        super.onDestroy();
    }

    /**
     * Set all messages in a given thread as read.
     */
    private void setRead() {
        if (conv != null) {
            ConversationListActivity.markRead(this, conv.getUri(), 1);
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.messagelist, menu);
        contactItem = menu.findItem(R.id.item_contact);
        if (conv != null) {
            setContactIcon(conv.getContact());
        }
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        if (p.getBoolean(PreferencesActivity.PREFS_HIDE_RESTORE, false)) {
            menu.removeItem(R.id.item_restore);
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in Action Bar clicked; go home
                Intent intent = new Intent(this, ConversationListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.item_delete_thread:
                ConversationListActivity.deleteMessages(this, uri, R.string.delete_thread_,
                        R.string.delete_thread_question, this);
                return true;
            case R.id.item_answer:
                send(true, false);
                return true;
            case R.id.item_call:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"
                            + conv.getContact().getNumber())));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "unable to open dailer", e);
                    Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.item_restore:
                etText.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(
                        PreferencesActivity.PREFS_BACKUPLASTTEXT, null));
                return true;
            case R.id.item_contact:
                if (conv != null && contactItem != null) {
                    WRAPPER.showQuickContactFallBack(this, contactItem.getActionView(), conv
                            .getContact().getLookUpUri(getContentResolver()), 2, null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        onItemLongClick(parent, view, position, id);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean onItemLongClick(final AdapterView<?> parent, final View view,
            final int position, final long id) {
        final Context context = this;
        final Message m = Message.getMessage(this, (Cursor) parent.getItemAtPosition(position));
        final Uri target = m.getUri();
        final int read = m.getRead();
        final int type = m.getType();
        Builder builder = new Builder(context);
        builder.setTitle(R.string.message_options_);

        final Contact contact = conv.getContact();
        final String a = contact.getNumber();
        Log.d(TAG, "p: ", a);
        final String n = contact.getName();

        String[] items = longItemClickDialog;
        if (TextUtils.isEmpty(n)) {
            items[WHICH_VIEW_CONTACT] = getString(R.string.add_contact_);
        } else {
            items[WHICH_VIEW_CONTACT] = getString(R.string.view_contact_);
        }
        items[WHICH_CALL] = getString(R.string.call) + " " + contact.getDisplayName();
        if (read == 0) {
            items = items.clone();
            items[WHICH_MARK_UNREAD] = context.getString(R.string.mark_read_);
        }
        if (type == Message.SMS_DRAFT) {
            items = items.clone();
            items[WHICH_FORWARD] = context.getString(R.string.send_draft_);
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                Intent i;
                switch (which) {
                    case WHICH_VIEW_CONTACT:
                        if (n == null) {
                            i = ContactsWrapper.getInstance().getInsertPickIntent(a);
                            Conversation.flushCache();
                        } else {
                            final Uri u = MessageListActivity.this.conv.getContact().getUri();
                            i = new Intent(Intent.ACTION_VIEW, u);
                        }
                        try {
                            MessageListActivity.this.startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "unable to launch dailer: ", i.getAction(), e);
                            Toast.makeText(MessageListActivity.this, R.string.error_unknown,
                                    Toast.LENGTH_LONG).show();
                        }
                        break;
                    case WHICH_CALL:
                        MessageListActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                .parse("tel:" + a)));
                        break;
                    case WHICH_MARK_UNREAD:
                        ConversationListActivity.markRead(context, target, 1 - read);
                        MessageListActivity.this.markedUnread = true;
                        break;
                    case WHICH_REPLY:
                        MessageListActivity.this.startActivity(ConversationListActivity
                                .getComposeIntent(MessageListActivity.this, a, false));
                        break;
                    case WHICH_FORWARD:
                        int resId;
                        if (type == Message.SMS_DRAFT) {
                            resId = R.string.send_draft_;
                            i = ConversationListActivity.getComposeIntent(MessageListActivity.this,
                                    MessageListActivity.this.conv.getContact().getNumber(), false);
                        } else {
                            resId = R.string.forward_;
                            i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra("forwarded_message", true);
                        }
                        CharSequence text;
                        if (PreferencesActivity.decodeDecimalNCR(context)) {
                            text = Converter.convertDecNCR2Char(m.getBody());
                        } else {
                            text = m.getBody();
                        }
                        i.putExtra(Intent.EXTRA_TEXT, text);
                        i.putExtra("sms_body", text);
                        context.startActivity(Intent.createChooser(i, context.getString(resId)));
                        break;
                    case WHICH_COPY_TEXT:
                        final ClipboardManager cm = (ClipboardManager) context
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        if (PreferencesActivity.decodeDecimalNCR(context)) {
                            cm.setText(Converter.convertDecNCR2Char(m.getBody()));
                        } else {
                            cm.setText(m.getBody());
                        }
                        break;
                    case WHICH_VIEW_DETAILS:
                        final int t = m.getType();
                        Builder b = new Builder(context);
                        b.setTitle(R.string.view_details_);
                        b.setCancelable(true);
                        StringBuilder sb = new StringBuilder();
                        final String a = m.getAddress(context);
                        final long d = m.getDate();
                        final String ds = DateFormat.format(
                                context.getString(R.string.DATEFORMAT_details), d).toString();
                        String sentReceived;
                        String fromTo;
                        if (t == Calls.INCOMING_TYPE) {
                            sentReceived = context.getString(R.string.received_);
                            fromTo = context.getString(R.string.from_);
                        } else if (t == Calls.OUTGOING_TYPE) {
                            sentReceived = context.getString(R.string.sent_);
                            fromTo = context.getString(R.string.to_);
                        } else {
                            sentReceived = "ukwn:";
                            fromTo = "ukwn:";
                        }
                        sb.append(sentReceived).append(" ");
                        sb.append(ds);
                        sb.append("\n");
                        sb.append(fromTo).append(" ");
                        sb.append(a);
                        sb.append("\n");
                        sb.append(context.getString(R.string.type_));
                        if (m.isMMS()) {
                            sb.append(" MMS");
                        } else {
                            sb.append(" SMS");
                        }
                        b.setMessage(sb.toString());
                        b.setPositiveButton(android.R.string.ok, null);
                        b.show();
                        break;
                    case WHICH_DELETE:
                        ConversationListActivity.deleteMessages(context, target,
                                R.string.delete_message_, R.string.delete_message_question, null);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.show();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    public final void onClick(final View v) {
        switch (v.getId()) {
            case R.id.send_:
                send(true, false);
                return;
            case R.id.text_paste:
                final CharSequence s = cbmgr.getText();
                etText.setText(s);
                return;
            default:
                // should never happen
        }
    }

    /**
     * {@inheritDoc}
     */
    public final boolean onLongClick(final View v) {
        switch (v.getId()) {
            case R.id.send_:
                send(false, true);
                return true;
            default:
                return true;
        }
    }

    /**
     * Build an {@link Intent} for sending it.
     *
     * @param autosend    autosend
     * @param showChooser show chooser
     * @return {@link Intent}
     */
    private Intent buildIntent(final boolean autosend, final boolean showChooser) {
        //noinspection ConstantConditions
        if (conv == null || conv.getContact() == null) {
            Log.e(TAG, "buildIntent() without contact: ", conv);
            throw new NullPointerException("conv and conv.getContact() must be not null");
        }
        final String text = etText.getText().toString().trim();
        final Intent i = ConversationListActivity.getComposeIntent(this, conv.getContact()
                .getNumber(), showChooser);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.putExtra("sms_body", text);
        if (autosend && enableAutosend && text.length() > 0) {
            i.putExtra("AUTOSEND", "1");
        }
        if (showChooser) {
            return Intent.createChooser(i, getString(R.string.reply));
        } else {
            return i;
        }
    }

    /**
     * Answer/send message.
     *
     * @param autosend    enable autosend
     * @param showChooser show chooser
     */
    private void send(final boolean autosend, final boolean showChooser) {
        try {
            final Intent i = buildIntent(autosend, showChooser);
            startActivity(i);
            //noinspection ConstantConditions
            PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .edit()
                    .putString(PreferencesActivity.PREFS_BACKUPLASTTEXT,
                            etText.getText().toString()).commit();
            etText.setText("");
        } catch (ActivityNotFoundException | NullPointerException e) {
            Log.e(TAG, "unable to launch sender app", e);
            Toast.makeText(this, R.string.error_sending_failed, Toast.LENGTH_LONG).show();
        }
    }
}
