package com.example.maris_tolstovs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
    Button clear_logs, execute_command_button, colorpicker;
    EditText command_label;
    TextView response;
    Spinner command_list;
    CheckBox dropdown_chk, manual_chk;
    public Handler mHandler;
    ProgressDialog loading_dialog;
    BroadcastReceiver mBroadcastReceiver1;
    Snackbar snack_not_connected;
    int colorpicker_color;

    //copy 10x10 matrix but by buttons
    Button grid[][] = new Button[10][10];

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
        colorpicker = findViewById(R.id.color_picker);

        //load commands into spinner
        String[] items = new String[]{
                "enter drawing mode",
                "3",
                "4",
                "5",
                "b",
                "a",
                "clear grid",
                "draw heart"
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
                            switch (command_list.getSelectedItem().toString()){
                                case "draw heart":
                                    drawAnmation(getResources().getStringArray(R.array.draw_heart));
                                    break;
                                case "enter drawing mode" :
                                    //repaint every button to grey
                                    clearButtonBackgrounds();
                                    changeMatrixStatus(true);
                                    //calls drawing command
                                    btt.write("2".getBytes());
                                    break;

                                case "clear grid" :
                                    //repaint every button to grey
                                    clearButtonBackgrounds();
                                    changeMatrixStatus(true);
                                    //calls clear command
                                    btt.write("c".getBytes());
                                    break;
                                default:
                                    //gets items string from selected dropdown item
                                    String sendtxt = command_list.getSelectedItem().toString();
                                    Log.i("[BLUETOOTH]", "Command:"+sendtxt);
                                    btt.write(sendtxt.getBytes());
                            }
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

        //pen colorpicker
        colorpicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialogBuilder
                        .with(MainActivity.this)
                        .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {
                            }
                        })
                        .setPositiveButton("ok", new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
//                                String val = Color.red(selectedColor)+","+Color.green(selectedColor)+","+Color.blue(selectedColor)+","+Color.alpha(selectedColor);
                                Log.e("[COLOR_SELECTOR]", "Selected color:"+ selectedColor);
                                colorpicker_color = selectedColor;
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //
                            }
                        })
                        .build()
                        .show();

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
                final Button btn = new Button(MainActivity.this);


                //each button text contains button id
                btn.setText(btn_id);
                //change background to grey
                btn.setBackgroundColor(Color.rgb(232,232,232));

                //each button listener
                btn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        String sendtxt, hexcolor;
                        //choose color
                        if (colorpicker_color != 0){
                            //color from colorpicker
                            sendtxt = btn_id+","+Color.red(colorpicker_color)+","+Color.green(colorpicker_color)+","+Color.blue(colorpicker_color)+",0";
                            btn.setBackgroundColor(Color.rgb(Color.red(colorpicker_color),Color.green(colorpicker_color),Color.blue(colorpicker_color)));
                        }else{
                            //red by default
                            sendtxt = btn_id+",250,0,0,0";
                            btn.setBackgroundColor(Color.rgb(255,0,0));
                        }

                        Log.i("[BLUETOOTH]", "Command:"+sendtxt);
                        //draw color
                        btt.write(sendtxt.getBytes());

                        //disables grid until recieves msg
                        changeMatrixStatus(false);
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
        //clears button backgrounds on reconnect
        clearButtonBackgrounds();
        //to eliminate white screen on bootup for few seconds
        new Handler().postDelayed(new Runnable() {
                public void run() {
                //gets BT adapter
                bta = BluetoothAdapter.getDefaultAdapter();

                //if bluetooth is not enabled then create Intent for user to turn it on
                if(bta == null || !bta.isEnabled()){
                    Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
                }else{
                    initiateBluetoothProcess();
                }
            }
        }, 300);
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()){
            //opens loading dialog
            loading_dialog = new ProgressDialog(MainActivity.this);
            loading_dialog.setMessage(getString(R.string.loading_text));
            loading_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loading_dialog.show();
            loading_dialog.setCancelable(false);

            //runnable stops app from freezing
            new Handler().postDelayed(new Runnable() {
                  public void run() {
                      //attempt to connect to bluetooth module
                      BluetoothSocket tmp = null;
                      mmDevice = bta.getRemoteDevice(MODULE_MAC);

                      //create socket
                      try {
                          tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                          mmSocket = tmp;
                          mmSocket.connect();

                          //closes snackbar on successful connect
                          if(snack_not_connected != null)snack_not_connected.dismiss();
                          Log.i("[BLUETOOTH]","Connected to: "+mmDevice.getName());
                      }catch(IOException e){
                          try{

                              if(mmSocket != null) mmSocket.close();

                              //if not connected snackbar on top
                              snack_not_connected = Snackbar.make(findViewById(R.id.main_view), getString(R.string.not_connected), Snackbar.LENGTH_INDEFINITE);
                              View view = snack_not_connected.getView();
                              FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view.getLayoutParams();
                              params.gravity = Gravity.TOP;
                              view.setLayoutParams(params);
                              snack_not_connected.setAction("RECONNECT", new View.OnClickListener() {
                                  @Override
                                  public void onClick(View view) {
                                      //reconnect
                                      connectToMatrix();
                                  }
                              });
                              snack_not_connected.show();
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
                                  if(response.getText().toString().length() >= 100){
                                      response.setText("");
                                      response.append(txt);
                                  }else{
                                      response.append("\n" + txt);
                                  }
                              }
                              //enables matrix
                              changeMatrixStatus(true);
                          }
                      };

                      Log.i("[BLUETOOTH]", "Creating and running Thread");
                      btt = new ConnectedThread(mmSocket,mHandler);
                      btt.start();

                      //close loading dialog
                      loading_dialog.dismiss();
                  }
              },100);


        }
    }

    //executes given string array
    public void drawAnmation(String leds[]){
        ProgressDialog anim_loading = new ProgressDialog(MainActivity.this);
        anim_loading.setMessage(getString(R.string.loading_anim));
        anim_loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        anim_loading.show();
        anim_loading.setCancelable(false);

        //executes each string in array
        for(String led : leds){
            Log.i("[BLUETOOTH]", "Command:"+led);
            btt.write(led.getBytes());
            try {
                //adds delay for each command
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                // code to resume or terminate...
            }
        }

        //closes animation dialog
        anim_loading.dismiss();
    }

    //clears button backgrounds
    public void clearButtonBackgrounds(){
        for(Button butn_row[] : grid)if(grid[0][0] != null)
            for(Button btn : butn_row)
                btn.setBackgroundColor(Color.rgb(232,232,232));
    }

    //clears button backgrounds
    public void changeMatrixStatus(boolean status){
        for(Button butn_row[] : grid)if(grid[0][0] != null)
            for(Button btn : butn_row)
                btn.setClickable(status);
    }
}
