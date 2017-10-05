package com.commandus.sms2gsheets;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andrei on 10/5/17.
 */

public class AddRequestTask extends AsyncTask<Void, Void, List<String>> {
    private static final String TAG = AddRequestTask.class.getSimpleName();
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;

    private Activity mActivity;
    private TextView mTextSign;
    private ContentLoadingProgressBar mProgress;
    private int mCode;

    AddRequestTask(
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
            return addLine();
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
     * @see https://developers.google.com/sheets/api/reference/rest/v4/spreadsheets.values/append
     */
    private List<String> addLine() throws IOException {
        String spreadsheetId = "1eYiu7IW8FpqT-PbYf_LM-nYMoNk8AKNp4jiDYAjAiCo";
        String range = "Class Data!A2:F";
        ValueRange vals = new ValueRange();
        vals.setMajorDimension("ROWS");
        List<List<Object>> vls;
        vls = new ArrayList<>();
        ArrayList<Object> row = new ArrayList<Object>();
        row.add("1");
        row.add("2");
        vls.add(row);
        vals.setValues(vls);
        Sheets.Spreadsheets.Values.Append r = this.mService.spreadsheets().values().append(spreadsheetId, range, vals);
        r.setValueInputOption("RAW");
        Log.i(TAG, r.toString());

        AppendValuesResponse response = r.execute();
        Log.i(TAG, response.toString());

        List<String> results = new ArrayList<>();
        results.add("Added");
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
