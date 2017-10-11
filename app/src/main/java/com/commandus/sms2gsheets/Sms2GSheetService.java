package com.commandus.sms2gsheets;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Sms2GSheetService extends Service {
    public static final String PAR_FROM = "from";
    public static final String PAR_BODY = "body";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private List<AppendRowsTask> mAppendRowsTasks = new ArrayList<>();

    private void startNextTask()
    {
        if (mAppendRowsTasks.size() > 0) {
            if (Helper.isDeviceOnline(this)) {
                AppendRowsTask t = mAppendRowsTasks.get(0);
                t.execute();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String from = extras.getString(PAR_FROM);
            String sms_body = extras.getString(PAR_BODY);
            Helper.showNotificationSMS(this, from, sms_body);
            logSMS(new Date(), from, sms_body);
        }
        return START_STICKY;
    }

    private void logSMS(Date date, String from, String sms_body) {
        ArrayList<String> row = new ArrayList<>();
        row.add(android.text.format.DateFormat.getDateFormat(this).format(date));
        row.add(android.text.format.DateFormat.getTimeFormat(this).format(date));
        row.add(from);
        row.add(sms_body);
        AppendRowsTask t = Helper.appendRow(this, row, mAppendRowsTasks);
        if (t == null) {
            Helper.showNotificationError(this, getString(R.string.err_no_credentials));
        } else {
            mAppendRowsTasks.add(t);
            startNextTask();
        }
    }

}
