package com.commandus.sms2gsheets;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 10/5/17.
 */

public class ListRequestTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;

    private Activity mActivity;
    private TextView mTextSign;
    private ContentLoadingProgressBar mProgress;
    private int mCode;

    ListRequestTask(
            Activity activity,
            int code,
            GoogleAccountCredential credential,
            TextView textSign,
            ContentLoadingProgressBar progress
    )
    {
        mActivity = activity;
        mCode = code;
        mTextSign = textSign;
        mProgress = progress;

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
    protected List<String> doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    /**
     * Fetch a list of names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     * @return List of names and majors
     * @throws IOException
     */
    private List<String> getDataFromApi() throws IOException {
        String spreadsheetId = "1eYiu7IW8FpqT-PbYf_LM-nYMoNk8AKNp4jiDYAjAiCo";
        String range = "Class Data!A2:F";
        List<String> results = new ArrayList<String>();
        ValueRange response = this.mService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values != null) {
            results.add("Student Name, Gender, Class Level, Home State, Major, Extracurricular Activity");
            for (List row : values) {
                results.add(row.get(0) + ", " + row.get(4));
            }
        }
        return results;
    }

    @Override
    protected void onPreExecute() {
        mTextSign.setText("");
        mProgress.show();
    }

    @Override
    protected void onPostExecute(List<String> output) {
        mProgress.hide();
        if (output == null || output.size() == 0) {
            mTextSign.setText("No results returned.");
        } else {
            output.add(0, "Data retrieved using the Google Sheets API:");
            mTextSign.setText(TextUtils.join("\n", output));
        }
    }

    @Override
    protected void onCancelled() {
        mProgress.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                Helper.showGooglePlayServicesAvailabilityErrorDialog(
                        mActivity,
                        mCode,
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                mActivity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        MainActivity.REQUEST_AUTHORIZATION);
            } else {
                mTextSign.setText("The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else {
            mTextSign.setText("Request cancelled.");
        }
    }

}
