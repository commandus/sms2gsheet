package com.commandus.sberbanksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null &&
                ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
            Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
            if (pduArray == null || pduArray.length == 0)
                return;
            SmsMessage[] messages = new SmsMessage[pduArray.length];
            for (int i = 0; i < pduArray.length; i++) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String format = intent.getStringExtra("format");
                    if (format == null)
                        format = "";
                    messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i], format);
                } else {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
                }
            }
            String sms_from = messages[0].getDisplayOriginatingAddress();
            if (sms_from.equalsIgnoreCase("900")) {
                StringBuilder bodyText = new StringBuilder();
                for (int i = 0; i < messages.length; i++) {
                    bodyText.append(messages[i].getMessageBody());
                }
                String body = bodyText.toString();
                Intent mIntent = new Intent(context, SberbankSmsService.class);
                mIntent.putExtra("body", body);
                context.startService(mIntent);
                abortBroadcast();
            }
        }
    }
}
