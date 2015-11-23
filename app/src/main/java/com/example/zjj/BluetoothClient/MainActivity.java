package com.example.zjj.BluetoothClient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LinearGradient;
import android.nfc.Tag;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zjj.BluetoothClient.R;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.logging.LogRecord;

import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.preference.PreferenceManager.OnActivityResultListener;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {
    Button IndoortoSemi, SemitoOutdoor, OutdoorroSemi, SemitoIndoor, IndoortoOutdoor, OutdoortoIndoor, Start, Stop;
    ArrayAdapter<String> listAdapter;
    ListView listView;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    public static UUID My_UUID = UUID.fromString("cfa37877-e7a1-41a8-9673-2b0844b5868f");
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    IntentFilter filter;
    BroadcastReceiver receiver;
    String tag = "debugging";
    public InputStream mmInStream;
    public OutputStream mmOutStream;
    public TextView txt;
    public static BluetoothDevice selectedDevice;
    public static BluetoothSocket btSocket;
    public ConnectThread connect;
    public ConnectedThread con;
    private boolean flag = false;
    private static final String Tag = "Debugging";
    private RadioButton rbtn;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SUCCESS_CONNECT:
                    // Do something
                    Log.d(Tag,"try to handle message");
                    con = new ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(),"CONNECT",Toast.LENGTH_SHORT).show();
                    rbtn.setChecked(true);
                    con.start();
                    String s = "successfully connected------message from client";
                    byte[] mesg = s.getBytes();
                    con.write(mesg);
                    Log.d(Tag,"successfully write a bytes");
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string,Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };



    public boolean CsvfileWriter(String data) throws IOException {
        Log.d(Tag,"start to write");
        long epoch = System.currentTimeMillis();
        Log.d(Tag, String.valueOf(epoch));
        //String path = Environment.getExternalStorageDirectory() + "/DataAnalyze.CSV";
        String path = "/storage/emulated/0/DataAnalyze.CSV";
        CSVWriter writer = new CSVWriter(new FileWriter(path,true));
        String timestamp = String.valueOf(epoch);
        String[] ss = (timestamp+","+data).split(",");
        writer.writeNext(ss);
        writer.flush();
        Log.d(Tag,"successfully wrote ground truth data");
        writer.close();
        return true;


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    /*
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, tmPhone, androidId;
        tmDevice =  tm.getDeviceId().toString();
        if (tm.getSimSerialNumber()==null){
            tmSerial = "000000000000000000";
        }
        else
        tmSerial =  tm.getSimSerialNumber().toString();
        androidId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID).toString();

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String uniqueId = deviceUuid.toString();
        Toast.makeText(getApplicationContext(),uniqueId,Toast.LENGTH_SHORT).show();
        txt.setText(uniqueId);
        My_UUID = UUID.fromString(uniqueId);*/
        if (btAdapter==null){
            //device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(), "No bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            if (!btAdapter.isEnabled()){
                turnOnBT();
            }
        }
        getPariedDevices();
        startDiscovery();
        if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent makeDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            makeDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 500);
            // In a real situation you would probably use startActivityForResult to get the user decision.
            startActivity(makeDiscoverable);
        }


    }

    private void startDiscovery() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    private void getPariedDevices() {
        Log.d(Tag,"try to get paired devices");
        devicesArray = btAdapter.getBondedDevices();
        Log.d(Tag,"got paired devices");
        if (devicesArray.size()>0){
            for (BluetoothDevice device:devicesArray){
                pairedDevices.add(device.getName());

            }
        }
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

    public void init(){
        // TODO Auto-generated method stub
        IndoortoSemi = (Button)findViewById(R.id.in_semi);
        SemitoOutdoor = (Button)findViewById(R.id.semi_out);
        OutdoorroSemi = (Button)findViewById(R.id.out_semi);
        SemitoIndoor = (Button)findViewById(R.id.semi_in);
        OutdoortoIndoor = (Button) findViewById(R.id.out_in);
        IndoortoOutdoor = (Button) findViewById(R.id.in_out);
        Start = (Button) findViewById(R.id.start_btn);
        Stop = (Button) findViewById(R.id.stop_btn);

        rbtn = (RadioButton) findViewById(R.id.con_sta);
        rbtn.setClickable(false);
        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);
        listView.setAdapter(listAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new  ArrayList<String>();
        //filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices = new ArrayList<BluetoothDevice>();
        Log.d(Tag,"Initialize all the components");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    String s = "";
                    for (int a = 0; a < pairedDevices.size(); a++){
                        if (device.getName().equals(pairedDevices.get(a))){
                            //append
                            Log.d(Tag,"assign string to paired devices");
                            s = "(PAIRED)";
                            break;
                        }
                    }
                    // matt-hp(paired)
                    listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    //run some code
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    //run some code

                }
                else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if (btAdapter.getState() == btAdapter.STATE_OFF){
                           turnOnBT();
                    }

                }
            }
        };



        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

        IndoortoSemi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Tag, "Indoor msg is on the way");
                String s = "#Indoor-Semi###";
                con = new ConnectedThread(btSocket);
                con.start();
                byte[] msg = (byte[]) s.getBytes();
                con.write(msg);

                String gt = "Indoor-SemiOutdoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag, "Indoor-SemiOutdoor msg is sent");
            }
        });

        SemitoOutdoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(Tag,"Semi-Outdoor msg is on the way");
                String s = "#Semi-Outdoor###";
                Log.d(Tag,"semi-outdoor connection is ready");


                 con = new ConnectedThread(btSocket);
                Log.d(Tag, "connected state1");
               // connectedThread.start();
               // Log.d(Tag,"write input");
                con.start();
                byte[] msg = (byte[]) s.getBytes();
                con.write(msg);

                String  gt = "SemiOutdoor-Outdoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"bt-socket check");
                if (btSocket!=null)
                Log.d(Tag,"Semi-outdoor msg is sent");
            }
        });


        OutdoorroSemi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Tag,"Outdoor msg is on the way");
                String s = "#Outdoor-Semi###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String  gt = "Outdoor-SemiOutdoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Outdoor-Semi msg is sent");
            }
        });


        SemitoIndoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Tag,"Semi-Indoor msg is on the way");
                String s = "#Semi-Indoor###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String  gt = "SemiOutdoor-Indoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Semi-Indoor msg is sent");
            }
        });


        IndoortoOutdoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Tag,"Indoor-Outdoor msg is on the way");
                String s = "#Indoor-Outdoor###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String gt = "Indoor-Outdoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Indoor-Outdoor msg is sent");
            }
        });


        OutdoortoIndoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Tag,"Outdoor-Indoor msg is on the way");
                String s = "#Outdoor-Indoor###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String gt = "Outdoor-Indoor";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Outdoor-Indoor msg is sent");
            }
        });


        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Tag,"Start msg is on the way");
                String s = "#Start###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String gt = "Start";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Start msg is sent");
            }
        });


        Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Tag,"Stop msg is on the way");
                String s = "#Stop###";

                con = new ConnectedThread(btSocket);
                con.start();
                //connected.start();
                byte[] msg = (byte[])s.getBytes();
                con.write(msg);

                String gt = "Stop";
                try {
                    CsvfileWriter(gt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(Tag,"Stop msg is sent");
            }
        });

    }



    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(),"bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        Log.d(Tag,"click TextView Item");
        if (btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }

        if (listAdapter.getItem(arg2).contains("PAIRED")){
            Log.d(Tag, "click the paired device");
            selectedDevice = devices.get(arg2);
            connect = new ConnectThread(selectedDevice);
            connect.start();
            /*
            if (flag) {
                String s = "initial message";
                byte[] msg = s.getBytes();
                Log.d(Tag, "try to see");
                con = new ConnectedThread(btSocket);
                Log.d(Tag, "ok, I see");
                con.write(msg);
                Log.d(Tag, "successfully write the initial msg");
                Log.d(Tag, "Trying to connect the server");
                Toast.makeText(getApplicationContext(), "Connection request is sent to  " +
                        "" + selectedDevice.getAddress(), Toast.LENGTH_SHORT).show();
            }*/
        }

    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                Log.d(Tag,"generate a UUID");
                tmp = device.createRfcommSocketToServiceRecord(My_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
            btSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(Tag, "socket is trying to connect");
                btSocket.connect();
                Log.d(Tag,"successfully connected to a server");
                flag = true;
               // rbtn.setChecked(true);
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    Log.d(Tag,"try to close socket");
                    btSocket.close();
                    Log.d(Tag,"socket is closed");
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)

            mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
        }



        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            Log.d(Tag,"Trying to get stream");
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                Log.d(Tag,"try to get Inputstream");
               tmpIn = socket.getInputStream();
                Log.d(Tag,"can get InputStream");
               tmpOut = socket.getOutputStream();
                Log.d(Tag,"can get OutputStream");
            } catch (IOException e) {
              Log.d(Tag,"fail to get any stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);
                    Log.d(Tag,"successfully read the input stream");
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            Log.d(Tag,"try to write output stream");
            try {

                Log.d(Tag,"get started to write bytes");
                mmOutStream.write(bytes);
                Log.d(Tag,"successfully write the outputStream");
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


}

