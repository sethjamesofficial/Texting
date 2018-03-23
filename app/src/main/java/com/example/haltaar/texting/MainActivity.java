package com.example.haltaar.texting;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> convoThreads = new ArrayList<>();
    ArrayList<Integer> threads = new ArrayList<>();
    ArrayList<String> numbers = new ArrayList<>();
    ListView convos;
    ArrayAdapter arrayAdapter;

    private static MainActivity inst;
    public static boolean active = false;
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int SEND_SMS_PERMISSIONS_REQUEST = 1;
    private static final int RECEIVE_SMS_PERMISSIONS_REQUEST = 1;
    private static final int READ_CONTACTS_PERMISSION_REQUEST = 1;
    private static final int MULTIPLE_PERMISSIONS_REQUEST = 1;

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    PendingIntent sentPI, deliveredPI;
    BroadcastReceiver smsSentReceiver, smsDeliveredReceiver;

    private static final String TAG = "MainActivity";
    
    FloatingActionButton fab;
    
    public static MainActivity instance() {
        return inst;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        convos = (ListView) findViewById(R.id.convos);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, convoThreads);
        convos.setAdapter(arrayAdapter);

        fab = (FloatingActionButton) findViewById(R.id.addFab);
        
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: fab pressed");

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                View alertView = getLayoutInflater().inflate(R.layout.dialog_new_message, null);
                final EditText inputNumber = (EditText) alertView.findViewById(R.id.inputNumber);

                Button alertButtonOkay = (Button) alertView.findViewById(R.id.buttonOkay);
                Button alertButtonCancel = (Button) alertView.findViewById(R.id.buttonCancel);

                alertBuilder.setView(alertView);
                final AlertDialog dialog = alertBuilder.create();

                alertButtonOkay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: new thread for: " + inputNumber.getText().toString());
                        Intent treadIntent = new Intent(getBaseContext(), ThreadActivity.class);
                        treadIntent.putExtra("EXTRA_THREAD_ID", 0);
                        treadIntent.putExtra("EXTRA_NUMBER", inputNumber.getText().toString());
                        treadIntent.putExtra("EXTRA_NAME", getContactName(getBaseContext(),inputNumber.getText().toString()));

                        startActivity(treadIntent);
                        dialog.dismiss();
                    }
                });

                alertButtonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();


            }
        });





        convos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent treadIntent = new Intent(getBaseContext(), ThreadActivity.class);
                treadIntent.putExtra("EXTRA_THREAD_ID", threads.get(position));
                treadIntent.putExtra("EXTRA_NUMBER", numbers.get(position));
                treadIntent.putExtra("EXTRA_NAME", getContactName(getBaseContext(), numbers.get(position)));

                startActivity(treadIntent);
            }
        });

        sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: permissions not granted, requesting permissions");
            getMultiplePermissions();
        } else {
            refreshConvos();
        }
    }

    public void refreshConvos() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");

        int indexThreadID = smsInboxCursor.getColumnIndex("thread_id");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexThreadID < 0 || !smsInboxCursor.moveToFirst()) return;
        arrayAdapter.clear();
        threads.clear();
        do {
            if (!threads.contains(smsInboxCursor.getInt(indexThreadID))) {
                threads.add(smsInboxCursor.getInt(indexThreadID));
                numbers.add(smsInboxCursor.getString(indexAddress));
                String str = getContactName(this, smsInboxCursor.getString(indexAddress)) +
                        "\n Thread ID:" + smsInboxCursor.getString(indexThreadID) + "\n";
                arrayAdapter.add(str);

            }

        } while (smsInboxCursor.moveToNext());
    }

    public static String getContactName(Context context, String phoneNo) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNo));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return phoneNo;
        }
        String Name = phoneNo;
        if (cursor.moveToFirst()) {
            Name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return Name;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                PopupMenu popup = new PopupMenu(this, findViewById(R.id.action_info));
                MenuInflater inflater = popup.getMenuInflater();
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.caeser_info:
                                Intent caeserBrowserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Caesar_cipher"));
                                startActivity(caeserBrowserIntent);
                                return true;
                            case R.id.aes_info:
                                Intent aesBrowserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Advanced_Encryption_Standard"));
                                startActivity(aesBrowserIntent);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                inflater.inflate(R.menu.info_popup, popup.getMenu());
                popup.show();                return true;

            case R.id.action_exit:
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }




    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        active = true;

        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS Sent!", Toast.LENGTH_SHORT).show();
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(MainActivity.this, "Generic Failure!", Toast.LENGTH_SHORT).show();
                        break;

                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(MainActivity.this, "No Service!", Toast.LENGTH_SHORT).show();
                        break;

                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(MainActivity.this, "Null PDU!", Toast.LENGTH_SHORT).show();
                        break;

                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(MainActivity.this, "Radio Off!", Toast.LENGTH_SHORT).show();
                        break;
                }

                if (MainActivity.active) {
                    MainActivity inst = MainActivity.instance();
                    inst.refreshConvos();
                } else if (ThreadActivity.active) {
                    ThreadActivity inst = ThreadActivity.instance();
                    inst.refreshThread();
                }
            }
        };

        smsDeliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS Delivered", Toast.LENGTH_SHORT).show();
                        break;

                    case Activity.RESULT_CANCELED:
                        Toast.makeText(MainActivity.this, "SMS Not Delivered!", Toast.LENGTH_SHORT).show();
                        break;
                }


            }
        };

        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
        registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED));

        refreshConvos();

    }

    @Override
    protected void onPause() {
        super.onPause();

        active = false;

        unregisterReceiver(smsDeliveredReceiver);
        unregisterReceiver(smsSentReceiver);
    }

    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_SMS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_SMS},
                    READ_SMS_PERMISSIONS_REQUEST);
        }
    }

    public void getPermissionToReceiveSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.RECEIVE_SMS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},
                    RECEIVE_SMS_PERMISSIONS_REQUEST);
        }
    }

    public void getPermissionToSendSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.SEND_SMS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                    SEND_SMS_PERMISSIONS_REQUEST);
        }
    }

    public void getPermissionToReadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    READ_CONTACTS_PERMISSION_REQUEST);
        }
    }

    public void getMultiplePermissions() {
        requestPermissions(new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS}, MULTIPLE_PERMISSIONS_REQUEST);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if (requestCode == MULTIPLE_PERMISSIONS_REQUEST) {
            if(grantResults.length == 4 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "All Permissions Granted", Toast.LENGTH_SHORT).show();
                refreshConvos();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
