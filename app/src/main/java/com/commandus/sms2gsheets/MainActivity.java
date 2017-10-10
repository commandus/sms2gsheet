package com.commandus.sms2gsheets;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int PERMISSIONS_REQUESTS = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String SHEET_RANGE = "A:C";

    GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private ApplicationSettings mSettings;

    private TextView mTextSign;
    private ContentLoadingProgressBar mProgress;

    private List<CreateSheetTask> mCreateSheetTasks;
    private List<AppendRowsTask> mAppendRowsTask;
    private FloatingActionButton mFabNewSpreadsheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCreateSheetTasks = new ArrayList<>() ;
        mAppendRowsTask = new ArrayList<>();
        mSettings = ApplicationSettings.getInstance(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mProgress = findViewById(R.id.progress_bar_sheets);

        setSupportActionBar(toolbar);

        mFabNewSpreadsheet = findViewById(R.id.fabNewSpreadsheet);
        mFabNewSpreadsheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.label_create_spreadsheet, Snackbar.LENGTH_LONG)
                        .setAction(R.string.label_add_spreadsheet, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                createSpreadsheet();
                                startNextTask();
                            }
                        })
                        .show();
            }
        });


        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "SMS receive granted");
        } else {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECEIVE_SMS)) {
                requestPermissionWithRationale();

            } else {
                requestPermissions();
            }
        }

        mTextSign = findViewById(R.id.text_sign);
        /*
        mTextSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTextSign.setEnabled(false);
                mTextSign.setText("");
                appendSms();
                startNextTask();
                mTextSign.setEnabled(true);
            }
        });
        */
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        invalidateSpreadsheet();
    }

    private void invalidateSpreadsheet() {
        String uid = mSettings.getAccountName();
        if ((uid != null) && (!uid.isEmpty())) {
            mFabNewSpreadsheet.setVisibility(View.VISIBLE);
        }
        else {
            mFabNewSpreadsheet.setVisibility(View.INVISIBLE);
        }

        String ss = mSettings.getSpreadsheetId();
        if ((ss != null) && (!ss.isEmpty())) {
            mTextSign.setMovementMethod(LinkMovementMethod.getInstance());
            String p = "<a href=\"https://docs.google.com/spreadsheets/d/" + ss + "/edit#gid=0\">"
                    + getString(R.string.link_spreadsheet) + "</a>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mTextSign.setText(Html.fromHtml(p, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mTextSign.setText(Html.fromHtml(p));
            }
            mTextSign.setEnabled(true);
            Log.i(TAG, p);
        }
        else {
            mTextSign.setEnabled(false);
        }
    }

    private void startNextTask()
    {
        if (mCreateSheetTasks.size() > 0) {
            if (checkNetworkNCredentials()) {
                CreateSheetTask t = mCreateSheetTasks.get(0);
                mCreateSheetTasks.remove(0);
                t.execute();
            }
        } else {
            if (mAppendRowsTask.size() > 0) {
                if (checkNetworkNCredentials()) {
                    AppendRowsTask t = mAppendRowsTask.get(0);
                    mAppendRowsTask.remove(0);
                    t.execute();
                }
            }
        }
    }

    public void requestPermissions() {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS},
                PERMISSIONS_REQUESTS);
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = getString(R.string.MSG_REQUEST_SMS_RECEIVE);
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestPermissions();
                        }
                    })
                    .show();
        } else {
            requestPermissions();
        }
    }

    private void createSpreadsheet() {
        // create a new spreadsheet
        mCreateSheetTasks.add(create_new());
    }

    private void appendHeader() {
        ArrayList<String> row = new ArrayList<>();
        row.add(getString(R.string.sheet_header_date));
        row.add(getString(R.string.sheet_header_from));
        row.add(getString(R.string.sheet_header_body));
        mAppendRowsTask.add(append_row(row));
    }

    private void appendSms() {
        ArrayList<String> row = new ArrayList<>();
        row.add(android.text.format.DateFormat.getDateFormat(this).format(new Date()));
        row.add("Unknown");
        row.add("Blah-blah-blah");
        mAppendRowsTask.add(append_row(row));
    }

    private void sendSMSMessage(String message) {
        String phoneNo = "900";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                requestPermissionWithRationale();
            } else {
                requestPermissions();
            }
        }
        else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkNetworkNCredentials() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return false;
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
            return false;
        } else if (!isDeviceOnline()) {
            mTextSign.setText(R.string.err_no_internet);
            return false;
        }
        return true;
    }

    /**
     * create a new spreadsheet
     */
    private CreateSheetTask create_new() {
        return new CreateSheetTask(mCredential,
                getString(R.string.format_new_spreadsheet_name),
                getString(R.string.format_new_sheet_name),
                new GoogleSheetsCreateResponseReceiver() {
            @Override
            public void onStart() {
                mTextSign.setText("");
                mProgress.show();
            }

            @Override
            public void onStop() {
                mProgress.hide();
            }

            @Override
            public void onCreate(Spreadsheet response) {
                if (response == null) {
                    mTextSign.setText(R.string.err_no_response);
                } else {
                    mTextSign.setText(response.toString());
                    mSettings.setSpreadsheetId(response.getSpreadsheetId());
                    List<Sheet> sheets = response.getSheets();
                    if (sheets.size() > 0) {
                        mSettings.setSheet(sheets.get(0).getProperties().getTitle());
                    }
                    else
                        mSettings.setSheet("");
                }
                mSettings.save(MainActivity.this);
                appendHeader();
                startNextTask();
            }

            @Override
            public void onCancel(Exception error) {
                if (error instanceof GooglePlayServicesAvailabilityIOException) {
                    Helper.showGooglePlayServicesAvailabilityErrorDialog(
                            MainActivity.this,
                            MainActivity.REQUEST_GOOGLE_PLAY_SERVICES,
                            ((GooglePlayServicesAvailabilityIOException) error)
                                    .getConnectionStatusCode());
                } else if (error instanceof UserRecoverableAuthIOException) {
                    MainActivity.this.startActivityForResult(
                            ((UserRecoverableAuthIOException) error).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    String m = getString(R.string.err_message) + error.getMessage();
                    mTextSign.setText(m);
                }
            }
        });
    }

    /**
     * append a new record to the spreadsheet
     */
    private AppendRowsTask append_row(
            final List<String> row
    ) {
        return new AppendRowsTask(mCredential, new GoogleSheetsAppendResponseReceiver() {
            @Override
            public AppendValuesResponse append_rows(Sheets sheets) throws IOException {
                String range;
                range = SHEET_RANGE;
                ValueRange vals = new ValueRange();
                vals.setMajorDimension("ROWS");
                List<List<Object>> vls;
                vls = new ArrayList<>();
                ArrayList<Object> ro = new ArrayList<>();
                for (String rr : row) {
                    ro.add(rr);
                }
                vls.add(ro);
                vals.setValues(vls);
                Sheets.Spreadsheets.Values.Append r = sheets.spreadsheets().values().append(
                        mSettings.getSpreadsheetId(), range, vals);
                r.setValueInputOption("RAW");
                AppendValuesResponse response = r.execute();
                Log.i(TAG, response.toString());
                return response;
            }

            @Override
            public void onStart() {
                mTextSign.setText("");
                mProgress.show();
            }

            @Override
            public void onStop() {
                mProgress.hide();
            }

            @Override
            public void onAppend(AppendValuesResponse response) {
                if (response == null) {
                    mTextSign.setText(R.string.err_no_response);
                } else {
                    mTextSign.setText(response.toString());
                }
                startNextTask();
            }

            @Override
            public void onCancel(Exception error) {
                if (error instanceof GooglePlayServicesAvailabilityIOException) {
                    Helper.showGooglePlayServicesAvailabilityErrorDialog(
                            MainActivity.this,
                            MainActivity.REQUEST_GOOGLE_PLAY_SERVICES,
                            ((GooglePlayServicesAvailabilityIOException) error)
                                    .getConnectionStatusCode());
                } else if (error instanceof UserRecoverableAuthIOException) {
                    MainActivity.this.startActivityForResult(
                            ((UserRecoverableAuthIOException) error).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    String m = getString(R.string.err_message) + error.getMessage();
                    mTextSign.setText(m);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUESTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "SMS receive granted");
            } else {
                Log.i(TAG, "SMS receive not granted");
            }
        }
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            Helper.showGooglePlayServicesAvailabilityErrorDialog(
                    MainActivity.this,
                    REQUEST_GOOGLE_PLAY_SERVICES,
                    connectionStatusCode
            );
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = mSettings.getAccountName();
            if ((accountName != null) && (!accountName.isEmpty())) {
                mCredential.setSelectedAccountName(accountName);
                startNextTask();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mTextSign.setText(R.string.err_no_playservices);
                } else {
                    startNextTask();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if ((accountName != null) && (!accountName.isEmpty())) {
                        mSettings.setAccountName(accountName);
                        mSettings.save(this);
                        mCredential.setSelectedAccountName(accountName);
                        startNextTask();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    startNextTask();
                }
                break;
        }
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }
}
