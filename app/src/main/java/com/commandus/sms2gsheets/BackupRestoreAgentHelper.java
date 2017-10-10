/*
 *
 */

package com.commandus.sms2gsheets;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class BackupRestoreAgentHelper extends BackupAgentHelper {
    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, ApplicationSettings.PREFS_NAME);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}