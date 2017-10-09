package com.commandus.sms2gsheets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by andrei on 10/5/17.
 */

public class Helper {
    /**
     * Display an error dialog showing that Google Play Services is missing or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    static void showGooglePlayServicesAvailabilityErrorDialog(
            Activity activity,
            int code,
            final int connectionStatusCode
    ) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog (
                activity,
                connectionStatusCode,
                code
        );
        dialog.show();
    }

}
