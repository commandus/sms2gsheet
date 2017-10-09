package com.commandus.sms2gsheets;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;

/**
 * Append rows to the Google Sheet
 */

public class AppendRowsTask extends AsyncTask<Void, Void, AppendValuesResponse> {
    private static final String TAG = AppendRowsTask.class.getSimpleName();
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;

    private GoogleSheetsResponseReceiver mReceiver;

    AppendRowsTask(
            GoogleAccountCredential credential,
            GoogleSheetsResponseReceiver receiver
    )
    {
        mReceiver = receiver;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Sheets API Android Quickstart")
                .build();
    }

    /**
     * Background task to call Google Sheets API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected AppendValuesResponse doInBackground(Void... params) {
        try {
            AppendValuesResponse response;
            return mReceiver.onRequest(this.mService);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        mReceiver.onStart();
    }

    @Override
    protected void onPostExecute(AppendValuesResponse output) {
        mReceiver.onStop();
        mReceiver.onAppend(output);
    }

    @Override
    protected void onCancelled() {
        mReceiver.onStop();
        if (mLastError != null) {
            mReceiver.onCancel(mLastError);
        }
    }

}
