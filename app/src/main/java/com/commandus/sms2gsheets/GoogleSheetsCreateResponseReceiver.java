package com.commandus.sms2gsheets;

import com.google.api.services.sheets.v4.model.Spreadsheet;

public interface GoogleSheetsCreateResponseReceiver {
    void onStart();
    void onStop();
    void onCreate(Spreadsheet response);
    void onCancel(Exception error);
}
