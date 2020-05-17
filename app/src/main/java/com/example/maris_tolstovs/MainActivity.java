package com.example.maris_tolstovs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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

        //opens loading dialog
        loading_dialog = new ProgressDialog(MainActivity.this);
        loading_dialog.setMessage(getString(R.string.loading_text));
        loading_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading_dialog.show();
        loading_dialog.setCancelable(false);

        //load commands into spinner
        String[] items = new String[]{
                "2",
                "3",
                "4",
                "5",
                "b",
                "a"
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
                Log.i("[BLUETOOTH]", "Attempting to send data");

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
            }
        });

        clear_logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                response.setText("cleared logs.");
            }
        });


        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if(!bta.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }else{
            initiateBluetoothProcess();
        }
        //clos eloading
        loading_dialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initiateBluetoothProcess();
        }
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
                try{mmSocket.close();}catch(IOException c){return;}
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
