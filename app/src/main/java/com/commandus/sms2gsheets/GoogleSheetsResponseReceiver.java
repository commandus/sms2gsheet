package com.commandus.sms2gsheets;

import com.google.api.services.sheets.v4.model.AppendValuesResponse;

import java.io.IOException;
import java.util.List;

/**
 * ListRequestTask interface
 * @see android.os.AsyncTask
 */

public interface GoogleSheetsResponseReceiver {
    AppendValuesResponse onRequest(com.google.api.services.sheets.v4.Sheets sheets) throws IOException;
    void onStart();
    void onStop();
    void onAppend(AppendValuesResponse response);
    void onCancel(Exception error);
}
