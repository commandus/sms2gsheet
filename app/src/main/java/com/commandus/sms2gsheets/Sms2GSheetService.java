package com.commandus.sms2gsheets;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Sms2GSheetService extends Service {
    public static final String PAR_FROM = "from";
    public static final String PAR_BODY = "body";
    private static final String TAG = Sms2GSheetService.class.getSimpleName();
    private Integer mSequence = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private HashMap<Integer, List<String> > mSMSRows = new HashMap<>();

    private void startNextTask()
    {
        if (!Helper.isDeviceOnline(this))
            return;
        for (Integer seq : mSMSRows.keySet()) {
            AppendRowsTask t = appendRow(seq, mSMSRows.get(seq));
            t.execute();
       }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String from = extras.getString(PAR_FROM);
                String sms_body = extras.getString(PAR_BODY);
                Helper.showNotificationSMS(this, from, sms_body);
                logSMS(new Date(), from, sms_body);
            }
        }
        return START_STICKY;
    }

    private void logSMS(Date date, String from, String sms_body) {
        ArrayList<String> sms_row = new ArrayList<>();
        sms_row.add(android.text.format.DateFormat.getDateFormat(this).format(date));
        sms_row.add(android.text.format.DateFormat.getTimeFormat(this).format(date));
        sms_row.add(from);
        sms_row.add(sms_body);
        mSequence++;
        mSMSRows.put(mSequence, sms_row);
        startNextTask();
    }

    /**
     * append a new record to the spreadsheet
     */
    private AppendRowsTask appendRow(
            final int sequence,
            final List<String> row) {
        final ApplicationSettings settings = ApplicationSettings.getInstance(Sms2GSheetService.this);
        String accountName = settings.getAccountName();
        if ((accountName == null) || (accountName.isEmpty())) {
            return null;
        }

        // Initialize credentials and service object.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(Sms2GSheetService.this, Arrays.asList(Helper.SCOPES))
                .setBackOff(new ExponentialBackOff());

        credential.setSelectedAccountName(accountName);

        return new AppendRowsTask(credential, new GoogleSheetsAppendResponseReceiver() {
            @Override
            public AppendValuesResponse append_rows(Sheets sheets) throws IOException {
                String range;
                range = Helper.SHEET_RANGE;
                ValueRange values = new ValueRange();
                values.setMajorDimension("ROWS");
                List<List<Object>> vls;
                vls = new ArrayList<>();
                ArrayList<Object> ro = new ArrayList<>();
                if (row != null) {
                    for (String rr : row) {
                        ro.add(rr);
                    }
                }
                vls.add(ro);
                values.setValues(vls);
                Sheets.Spreadsheets.Values.Append r = sheets.spreadsheets().values().append(
                        settings.getSpreadsheetId(), range, values);
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
                    Helper.showNotificationError(Sms2GSheetService.this, getString(R.string.err_no_response));
                    Log.e(TAG, getString(R.string.err_no_response));
                } else {
                    Log.i(TAG, getString(R.string.msg_response) + response.toString());
                }
                if (mSMSRows != null) {
                    if (!mSMSRows.containsKey(sequence)) {
                        Log.e(TAG, "Invalid sequence");
                    } else {
                        mSMSRows.remove(sequence);
                    }
                }
            }

            @Override
            public void onCancel(Exception error) {
                if (error instanceof GooglePlayServicesAvailabilityIOException) {
                    Helper.showNotificationError(Sms2GSheetService.this, getString(R.string.err_no_playservices));
                    Log.e(TAG, getString(R.string.err_no_playservices));
                } else if (error instanceof UserRecoverableAuthIOException) {
                    Helper.showNotificationError(Sms2GSheetService.this, getString(R.string.err_no_credentials));
                    Log.e(TAG, getString(R.string.err_no_credentials));
                } else {
                    Helper.showNotificationError(Sms2GSheetService.this, getString(R.string.err_message) + error.getMessage());
                    Log.e(TAG, getString(R.string.err_message) + error.getMessage());
                }
            }
        });
    }

}
