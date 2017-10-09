package com.commandus.sms2gsheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;

import java.io.IOException;

/**
 * ListRequestTask interface
 * @see android.os.AsyncTask
 */

public interface GoogleSheetsAppendResponseReceiver {
    AppendValuesResponse append_rows(Sheets sheets) throws IOException;
    void onStart();
    void onStop();
    void onAppend(AppendValuesResponse response);
    void onCancel(Exception error);
}
