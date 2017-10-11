package com.commandus.sms2gsheets;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Helper {
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    static final String SHEET_RANGE = "A:D";
    private static final String TAG = Helper.class.getSimpleName();
    private static int icon = R.drawable.ic_stat_512x512;

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    static boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


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

    static void showNotificationSMS(Context context, String from, String body) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        Notification.Builder builder;
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = context.getString(R.string.notification_channel_id);
            NotificationChannel ch = new NotificationChannel(
                    channelId, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(ch);
            builder = new Notification.Builder(context, channelId)
                    .setContentTitle(context.getString(R.string.msg_from) + from)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(icon)
                    .setAutoCancel(true);
        } else {
            builder = new Notification.Builder(context)
                    .setContentTitle(context.getString(R.string.msg_from) + from)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(icon)
                    .setAutoCancel(true);
        }

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }

        notificationManager.notify(icon, notification);
    }

    static void showNotificationError(Context context, String error) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.err_message) + error)
                .setContentText(error)
                .setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        notificationManager.notify(icon, notification);
    }

    /**
     * append a new record to the spreadsheet
     */
    static AppendRowsTask appendRow(
            final Context context,
            final List<String> row,
            final List<AppendRowsTask> taskList) {
        final ApplicationSettings settings = ApplicationSettings.getInstance(context);
        String accountName = settings.getAccountName();
        if ((accountName == null) || (accountName.isEmpty())) {
            return null;
        }

        // Initialize credentials and service object.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

            credential.setSelectedAccountName(accountName);

        return new AppendRowsTask(credential, new GoogleSheetsAppendResponseReceiver() {
            @Override
            public AppendValuesResponse append_rows(Sheets sheets) throws IOException {
                String range;
                range = SHEET_RANGE;
                ValueRange vals = new ValueRange();
                vals.setMajorDimension("ROWS");
                List<List<Object>> vls;
                vls = new ArrayList<>();
                ArrayList<Object> ro = new ArrayList<>();
                for (String rr : row) {
                    ro.add(rr);
                }
                vls.add(ro);
                vals.setValues(vls);
                Sheets.Spreadsheets.Values.Append r = sheets.spreadsheets().values().append(
                        settings.getSpreadsheetId(), range, vals);
                r.setValueInputOption("RAW");
                AppendValuesResponse response = r.execute();
                Log.i(TAG, response.toString());
                return response;
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onAppend(AppendValuesResponse response) {
                if (response == null) {
                    Helper.showNotificationError(context, context.getString(R.string.err_no_response));
                    Log.e(TAG, context.getString(R.string.err_no_response));
                } else {
                    Log.i(TAG, context.getString(R.string.msg_response) + response.toString());
                }
                if (taskList != null) {
                    if (taskList.size() > 0)
                        taskList.remove(0);
                }
            }

            @Override
            public void onCancel(Exception error) {
                if (error instanceof GooglePlayServicesAvailabilityIOException) {
                    Helper.showNotificationError(context, context.getString(R.string.err_no_playservices));
                    Log.e(TAG, context.getString(R.string.err_no_playservices));
                } else if (error instanceof UserRecoverableAuthIOException) {
                    Helper.showNotificationError(context, context.getString(R.string.err_no_credentials));
                    Log.e(TAG, context.getString(R.string.err_no_credentials));
                } else {
                    Helper.showNotificationError(context, context.getString(R.string.err_message) + error.getMessage());
                    Log.e(TAG, context.getString(R.string.err_message) + error.getMessage());
                }
            }
        });
    }

}
