package de.ub0r.android.smsdroid;

import android.app.Activity;
import android.content.Intent;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;

import java.net.MalformedURLException;
import java.net.URL;

import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.logg0r.Log;

public class ConsentManager {

    private static final String TAG = "ConsentManager";
    private static final String ADMOB_PUBLISHER_ID = "pub-1948477123608376";
    private static final String PRIVACY_URL = "https://github.com/felixb/smsdroid/blob/master/PRIVACY.md";

    private final Activity mActivity;
    private ConsentForm mForm;

    public ConsentManager(final Activity activity) {
        mActivity = activity;
    }

    public void updateConsent() {
        if (!DonationHelper.hideAds(mActivity)) {
            ConsentInformation consentInformation = ConsentInformation.getInstance(mActivity);
            String[] publisherIds = {ADMOB_PUBLISHER_ID};
            consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
                @Override
                public void onConsentInfoUpdated(final ConsentStatus consentStatus) {
                    Log.i(TAG, "updated consent status: ", consentStatus.toString());
                    checkConsentForAds();
                }

                @Override
                public void onFailedToUpdateConsentInfo(final String errorDescription) {
                    Log.e(TAG, "failed to update consent info: ", errorDescription);
                }
            });
        }
    }

    public boolean showAds() {
        return !DonationHelper.hideAds(mActivity) && checkConsentForAds();
    }

    public boolean needConsent() {
        final ConsentInformation consentInformation = ConsentInformation.getInstance(mActivity);
        if (!consentInformation.isRequestLocationInEeaOrUnknown()) {
            Log.d(TAG, "User is outside EEA");
            return false;
        } else {
            return true;
        }
    }

    private boolean checkConsentForAds() {
        final ConsentInformation consentInformation = ConsentInformation.getInstance(mActivity);
        if (!consentInformation.isRequestLocationInEeaOrUnknown()) {
            Log.d(TAG, "User is outside EEA");
            return true;
        }

        final ConsentStatus consentStatus = consentInformation.getConsentStatus();
        Log.d(TAG, "ConstentStatus: ", consentStatus);

        if (ConsentStatus.UNKNOWN.equals(consentStatus)) {
            Log.d(TAG, "Need to ask for consent");
            // we need to ask for consent
            askForConsent();
            return false;
        }

        return true;
    }

    public void askForConsent() {
        URL privacyUrl = null;
        try {
            privacyUrl = new URL(PRIVACY_URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error parsing privacy url", e);
            mActivity.finish();
        }
        mForm = new ConsentForm.Builder(mActivity, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        Log.d(TAG, "onConsentFormLoaded");
                        mForm.show();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        Log.d(TAG, "onConsentFormOpened");
                    }

                    @Override
                    public void onConsentFormClosed(final ConsentStatus consentStatus, final Boolean userPrefersAdFree) {
                        Log.d(TAG, "onConsentFormClosed(", consentStatus, ", ", userPrefersAdFree, ")");
                        if (userPrefersAdFree) {
                            Intent i = new Intent(Intent.ACTION_VIEW, DonationHelper.DONATOR_URI);
                            mActivity.startActivity(i);
                        } else if (ConsentStatus.UNKNOWN.equals(consentStatus)) {
                            mActivity.finish();
                        }
                    }

                    @Override
                    public void onConsentFormError(final String errorDescription) {
                        Log.e(TAG, "error showing consent form: ", errorDescription);
                    }
                })
                .withPersonalizedAdsOption()
                .withAdFreeOption()
                .build();
        mForm.load();
        mForm.show();
    }
}
