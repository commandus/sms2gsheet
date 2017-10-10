package com.commandus.sms2gsheets;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;

/**
 * Append rows to the Google Sheet
 */

class CreateSheetTask extends AsyncTask<Void, Void, Spreadsheet> {
    private static final String TAG = CreateSheetTask.class.getSimpleName();
    private Sheets mService = null;
    private Exception mLastError = null;

    private GoogleSheetsCreateResponseReceiver mReceiver;
    private String mTitle;

    CreateSheetTask(
            GoogleAccountCredential credential,
            String title,
            String sheetTitle,
            GoogleSheetsCreateResponseReceiver receiver
    )
    {
        mReceiver = receiver;
        mTitle = title;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(TAG)
                .build();
    }

    /**
     * Background task to call Google Sheets API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected Spreadsheet doInBackground(Void... params) {
        try {
            Spreadsheet requestBody = new Spreadsheet();
            SpreadsheetProperties props = new SpreadsheetProperties();
            props.setTitle(mTitle);
            requestBody.setProperties(props);
            Sheets.Spreadsheets.Create request = mService.spreadsheets().create(requestBody);
            return request.execute();

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
    protected void onPostExecute(Spreadsheet output) {
        mReceiver.onStop();
        mReceiver.onCreate(output);
    }

    @Override
    protected void onCancelled() {
        mReceiver.onStop();
        if (mLastError != null) {
            mReceiver.onCancel(mLastError);
        }
    }

}
