package com.example.maris_tolstovs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public final static String MODULE_MAC = "98:D3:51:F9:4E:78";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    Button clear_logs, execute_command_button;
    EditText command_label;
    TextView response;
    Spinner command_list;
    CheckBox dropdown_chk, manual_chk;
    public Handler mHandler;
    ProgressDialog loading_dialog;
    BroadcastReceiver mBroadcastReceiver1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("[BLUETOOTH]", "Creating listeners");

        //point where is every object in view
        response = findViewById(R.id.response);
        execute_command_button = findViewById(R.id.run_command);
        command_label = findViewById(R.id.command_field);
        clear_logs = findViewById(R.id.clear_logs);
        command_list = findViewById(R.id.command_list);
        dropdown_chk = findViewById(R.id.chkbox_dropdown);
        manual_chk = findViewById(R.id.chkbox_manual);

        //load commands into spinner
        String[] items = new String[]{
                "2",
                "3",
                "4",
                "5",
                "b",
                "a",
                "c"
        };

        //adapter for dropdown to show it in view
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);

        command_list.setAdapter(adapter);
        command_label.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //empty required override method
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //on text change
                //changes checkboxes to active option
                if(!manual_chk.isChecked()){
                    manual_chk.setChecked(true);
                    dropdown_chk.setChecked(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //empty required override method
            }
        });

        command_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                //on option selection

                //changes checkboxes to active option
                if(!dropdown_chk.isChecked()){
                    manual_chk.setChecked(false);
                    dropdown_chk.setChecked(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //empty required override method
            }
        });

        //run command buton onclick function
        execute_command_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btt.isAlive()){
                    Log.i("[BLUETOOTH]", "Attempting to send data");
//                Log.e("name", btt.isAlive()+" ");
                    //if we have connection to the bluetoothmodule
                    if (mmSocket.isConnected() && btt != null) {
                        //if manual command mode is selected
                        if(manual_chk.isChecked()){
                            //reads entered command
                            String sendtxt = command_label.getText().toString();
                            Log.i("[BLUETOOTH]", "Command:"+sendtxt);
                            btt.write(sendtxt.getBytes());
                            //if dropdown commands checkbox is checked
                        }else if(dropdown_chk.isChecked()){
                            //gets items string from selected dropdown item
                            String sendtxt = command_list.getSelectedItem().toString();
                            Log.i("[BLUETOOTH]", "Command:"+sendtxt);
                            btt.write(sendtxt.getBytes());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                }else{
                    //tries to reconnect if BT conn is lost on execute
                    connectToMatrix();
                }

            }
        });

        clear_logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                response.setText("cleared logs.");
            }
        });

        //to eliminate white screen on bootup for few seconds
        connectToMatrix();

        //The BroadcastReceiver that listens for bluetooth broadcasts
        mBroadcastReceiver1 = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch(state) {
                        case BluetoothAdapter.STATE_OFF:
                            connectToMatrix();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            connectToMatrix();
                            break;
                    }
                }
            }
        };

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        generateButtonGrid();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initiateBluetoothProcess();
        }
    }

    public void generateButtonGrid(){
        //the layout on which you are working
        LinearLayout linear = (LinearLayout) findViewById(R.id.buttongrid_view);

        Button grid[][] = new Button[10][10];

        //for each row
        for(int i = 9; i >= 0; i--){
            //create row layout
            LinearLayout lin = new LinearLayout(MainActivity.this);
            lin.setOrientation(LinearLayout.HORIZONTAL);
            //for each column
            for(int j = 0; j < grid[i].length ; j++) {
                //button id
                final String btn_id = (i*10)+j+"";

                //create button
                Button btn = new Button(MainActivity.this);
                btn.setPadding(0,0,0,0);

                //each button text contains button id
                btn.setText(btn_id);

                //each button listener
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        String sendtxt = btn_id+",250,0,0,0";
                        Log.i("[BLUETOOTH]", "Command:"+sendtxt);
                        //draw color
                        btt.write(sendtxt.getBytes());
//                        btn.setBackgroundColor(Color.parseColor("#d1431b"));
                    }
                });
                //add button to array
                grid[i][j] = btn;
                //add button to rows view
                lin.addView(grid[i][j]);
            }
            //add each row into certical list view
            linear.addView(lin);
        }
    }

    public void connectToMatrix(){
        //to eliminate white screen on bootup for few seconds
        new Handler().postDelayed(new Runnable() {
                public void run() {
                //gets BT adapter
                bta = BluetoothAdapter.getDefaultAdapter();

                //if bluetooth is not enabled then create Intent for user to turn it on
                if(!bta.isEnabled()){
                    Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
                }else{
                    //opens loading dialog
                    loading_dialog = new ProgressDialog(MainActivity.this);
                    loading_dialog.setMessage(getString(R.string.loading_text));
                    loading_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    loading_dialog.show();
                    loading_dialog.setCancelable(false);
                    initiateBluetoothProcess();
                    loading_dialog.dismiss();
                }
            }
        }, 300);
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()){

            //attempt to connect to bluetooth module
            BluetoothSocket tmp = null;
            mmDevice = bta.getRemoteDevice(MODULE_MAC);

            //create socket
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();
                Log.i("[BLUETOOTH]","Connected to: "+mmDevice.getName());
            }catch(IOException e){
                try{
                    mmSocket.close();
                }catch(IOException c){
                    return;
                }
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        if(response.getText().toString().length() >= 30){
                            response.setText("");
                            response.append(txt);
                        }else{
                            response.append("\n" + txt);
                        }
                    }
                }
            };

            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
        }
    }
}
