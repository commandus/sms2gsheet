package com.commandus.sms2gsheets;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int PERMISSIONS_REQUESTS = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.RECEIVE_SMS};  // , Manifest.permission.SEND_SMS;

    GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private ApplicationSettings mSettings;

    private TextView mTextSign;
    private ContentLoadingProgressBar mProgress;

    private int mNeedCreateNewSpreadsheet;
    private Integer mSequence = 0;
    private HashMap<Integer, List<String> > mSMSRows;
    private FloatingActionButton mFabNewSpreadsheet;

    /**
     * < Android 22
     * https://stackoverflow.com/questions/37312103/unable-to-get-provider-com-google-firebase-provider-firebaseinitprovider
     */
    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Realm.init(this); // initialize other plugins

        mNeedCreateNewSpreadsheet = 0;
        mSMSRows = new HashMap<>();
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
                                // create a new spreadsheet
                                mNeedCreateNewSpreadsheet = 1;
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
            mTextSign.setOnClickListener(null);
            mTextSign.setMovementMethod(LinkMovementMethod.getInstance());
            String p = "<a href=\"https://docs.google.com/spreadsheets/d/" + ss + "/edit#gid=0\">"
                    + getString(R.string.link_spreadsheet) + "</a>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mTextSign.setText(Html.fromHtml(p, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mTextSign.setText(Html.fromHtml(p));
            }
            mTextSign.setEnabled(true);
        }
        else {
            String l = getString(R.string.label_create_spreadsheet);
            String p = "<font color=\"blue\"><b><u>" + l + "</u></b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mTextSign.setText(Html.fromHtml(p, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mTextSign.setText(Html.fromHtml(p));
            }
            mTextSign.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // create a new spreadsheet
                    mTextSign.setEnabled(false);
                    mNeedCreateNewSpreadsheet = 1;
                    startNextTask();
                }
            });
            mTextSign.setEnabled(true);
        }
    }

    private void startNextTask()
    {
        invalidateSpreadsheet();
        if (!checkNetworkNCredentials()) {
            return;
        }
        if (mNeedCreateNewSpreadsheet == 1) {
            mNeedCreateNewSpreadsheet = 2;  // in progress already, do not start twice!
            CreateSheetTask t = create_new();
            t.execute();
        } else {
            for (Integer seq : mSMSRows.keySet()) {
                AppendRowsTask t = append_row(seq, mSMSRows.get(seq));
                t.execute();
                break; // just first one, after success it starts again
            }
        }
    }

    public void requestPermissions() {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,  
                PERMISSIONS_REQUESTS);
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = getString(R.string.MSG_REQUEST_SMS_RECEIVE);
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.label_grant), new View.OnClickListener() {
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

    /*
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
    */

    private boolean checkNetworkNCredentials() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
            return false;
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
            return false;
        } else if (!Helper.isDeviceOnline(this)) {
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
                mTextSign.setText(getString(R.string.msg_creating_spreadsheet));
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

                // Add task to add header
                ArrayList<String> sms_row = new ArrayList<>();
                sms_row.add(getString(R.string.sheet_header_date));
                sms_row.add(getString(R.string.sheet_header_time));
                sms_row.add(getString(R.string.sheet_header_from));
                sms_row.add(getString(R.string.sheet_header_body));
                mSequence++;
                mSMSRows.put(mSequence, sms_row);

                // no more need to create spreadsheet
                mNeedCreateNewSpreadsheet = 0;
                startNextTask();    // invalidate
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
                    String m = getString(R.string.err_create_spreadsheet) + "\n" + error.toString();
                    mTextSign.setText(m);
                    Log.e(TAG, m);
                }
                mNeedCreateNewSpreadsheet = 0;
            }
        });
    }

    /**
     * append a new record to the spreadsheet
     */
    private AppendRowsTask append_row(
            final int sequence,
            final List<String> row) {
        return new AppendRowsTask(mCredential, new GoogleSheetsAppendResponseReceiver() {
            @Override
            public AppendValuesResponse append_rows(Sheets sheets) throws IOException {
                String range;
                range = Helper.SHEET_RANGE;
                ValueRange values = new ValueRange();
                values.setMajorDimension("ROWS");
                List<List<Object>> vls;
                vls = new ArrayList<>();
                ArrayList<Object> ro = new ArrayList<>();
                if (row != null) {
                    for (String rr : row) {
                        ro.add(rr);
                    }
                }
                vls.add(ro);
                values.setValues(vls);
                Sheets.Spreadsheets.Values.Append r = sheets.spreadsheets().values().append(
                        mSettings.getSpreadsheetId(), range, values);
                r.setValueInputOption("RAW");
                AppendValuesResponse response = r.execute();
                Log.i(TAG, response.toString());
                return response;
            }

            @Override
            public void onStart() {
                mTextSign.setText(R.string.msg_appending_header);
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
                    mTextSign.setText(R.string.msg_spreadsheet_created);
                }
                if (mSMSRows != null) {
                    if (!mSMSRows.containsKey(sequence)) {
                        Log.e(TAG, "Invalid sequence");
                    } else {
                        mSMSRows.remove(sequence);
                    }
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
        MenuItem mi = menu.findItem(R.id.action_tts_on);
        if (mi != null) {
            mi.setChecked(mSettings.isTTSOn());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_visit_site:
                String url = getString(R.string.url_site_help);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                return true;
            case R.id.action_tts_on:
                item.setChecked(!item.isChecked());
                mSettings.setTTSOn(this, item.isChecked());
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
                    getString(R.string.msg_request_account),
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
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                }
                break;
        }
        startNextTask();
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
        startNextTask();
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
