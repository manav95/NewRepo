package transapps.android_blutooth;

import android.graphics.PixelFormat;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.graphics.BitmapFactory;
import android.app.Activity;
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
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.glass.content.Intents;
import android.graphics.Bitmap;
import java.io.File;
import android.os.FileObserver;
import com.affectiva.android.affdex.sdk.Frame;
import java.io.ByteArrayOutputStream;
import com.affectiva.android.affdex.sdk.Frame.ROTATE;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

public class BluetoothClient extends Activity {

    public static final int READY_TO_CONN = 0;
    public static final int CANCEL_CONN = 1;
    public static final int MESSAGE_READ = 2;
    static File currFile;
    private SurfaceView cameraView; //SurfaceView used to display camera images
    private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
    int cameraPreviewWidth = 0;
    int cameraPreviewHeight = 0;
    private Frame mostRecentFrame;
    private ImageView view;
    private ImageView im;
    private ImageView secView;
    private TextView firstTextView;
    private TextView secondTextView;
    // holds the bluetooth names/ids that we're associated with.
    ArrayAdapter<String> btArray;
    // bt adapter for all your bt needs
    BluetoothAdapter myBt;
    String NAME = "G6BITCHES";
    String TAG = "G6 Bluetooth Slave Activity";
    UUID[] uuids = new UUID[2];
    // some uuid's we like to use..
    String uuid1 = "05f2934c-1e81-4554-bb08-44aa761afbfb";
    String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";
    //  DateFormat df = new DateFormat("ddyyyy")
    ConnectThread mConnThread;
    Spinner devices;
    Handler handle;
    // constant we define and pass to startActForResult (must be >0), that the system passes back to you in your onActivityResult() 
    // implementation as the requestCode parameter.
    int REQUEST_ENABLE_BT = 1;
    int TAKE_PICTURE_REQUEST = 2;
    // bc for discovery mode for BT...
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (device != null) {
                    //if (device.getName().contains("Nexus")) {
                    //    btArray.add(device.getName() + "\n" + device.getAddress());
                        btArray.add(device.getName() + "\n" + device.getAddress());
                }
                //update();
            }
        }
    };

    Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
//        ActxfionBar actionBar = getActionBar();
//        actionBar.hide();
        view = (ImageView) findViewById(R.id.imageView);
        secView = (ImageView) findViewById(R.id.imageView2);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        cameraView = (SurfaceView) findViewById(R.id.camera_preview);
        cameraView.setZOrderOnTop(true);
        cameraView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        im = (ImageView) findViewById(R.id.camera_image);
        firstTextView = (TextView) findViewById(R.id.textView);
        secondTextView = (TextView) findViewById(R.id.textView2);
        firstTextView.setText("0");
        secondTextView.setText("0");

        //      publishCards(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ctx = this;
        handle = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case READY_TO_CONN:
                        mConnThread = null;
                        update();
                        break;
                    case CANCEL_CONN:
                        break;
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;

                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        Log.e(TAG, "received: " + readMessage);
                        if (readMessage.length() > 0) {
                            // do soemthing...
                        }


                        //                      updateCards(ctx, readMessage);
                        //                          update()
                        //  mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);

                        break;
                    default:
                        break;
                }
            }
        };
        btArray = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        btArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uuids[0] = UUID.fromString(uuid1);
        uuids[1] = UUID.fromString(uuid2);
        // spinner for displaying available devices for pairing
        //devices = (Spinner) findViewById(R.id.devices_spinner);
        //devices.setAdapter(btArray);
        // use the same UUID across an installation
        // should allow clients to find us repeatedly
        myBt = BluetoothAdapter.getDefaultAdapter();
        if (myBt == null) {
            Toast.makeText(this, "Device Does not Support Bluetooth", Toast.LENGTH_LONG).show();
        } else if (!myBt.isEnabled()) {
            // we need to wait until bt is enabled before set up, so that's done either in the following else, or 
            // in the onActivityResult for our code ...
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            detectAndSetUp();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Failed to enable Bluetooth", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_LONG).show();
                detectAndSetUp();
            }
        }
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            processPictureWhenReady(picturePath);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
           currFile = pictureFile;
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }
    private void detectAndSetUp() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        Set<BluetoothDevice> pairedDevices = myBt.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {

                if(device.getName().contains("Nexus")) {
                    btArray.add(device.getName() + "\n" + device.getAddress());
                } else {
                    btArray.add(device.getName() + "\n" + device.getAddress());
                    update();

                }
                // Add the name and address to an array adapter to show in a ListView
                //              btArray.add(device.getName() + "\n" + device.getAddress());
                //              update();
            }
        }
        myBt.startDiscovery();
    }

    public void update() {
        devices = (Spinner) findViewById(R.id.devices_spinner);
        devices.setAdapter(btArray);
        devices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long id) {
                if(mConnThread!=null) {
                    Log.e(TAG,"Canceling old connection, and starting new one.");
                    mConnThread.cancel();
                } else {
                    Log.e(TAG,"got a thing...");
                    String str = ((TextView)arg1).getText().toString();
                    Log.e(TAG,"tots: "+str);
                    String[] vals = str.split("\n");
                    Log.e(TAG,"mac: "+vals[1]);
                    BluetoothDevice dev = myBt.getRemoteDevice(vals[1]);
                    mConnThread = new ConnectThread(dev);
                    mConnThread.run();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bluetooth, menu);
        return true;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.e(TAG,"ConnectThread start....");
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {

                // this seems to work on the note3...
                // you can remove the Insecure if you want to... 
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuids[0]);
                //                  Method m;
                // this is an approach I've seen others use, it wasn't nescesary for me,
                // but your results may vary...

                //                  m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                //                  tmp = (BluetoothSocket) m.invoke(device, 1);
                //              } catch (NoSuchMethodException e1) {
                //                  // TODO Auto-generated catch block
                //                  e1.printStackTrace();
                //              } catch (IllegalArgumentException e2) {
                //                  // TODO Auto-generated catch block
                //                  e2.printStackTrace();
                //              } catch (IllegalAccessException e3) {
                //                  // TODO Auto-generated catch block
                //                  e3.printStackTrace();
                //              } catch (InvocationTargetException e4) {
                //                  // TODO Auto-generated catch block
                //                  e4.printStackTrace();
                //              }   
                //                  if(tmp.isConnected()) {
                //                      break
                //                  }



            } catch (Exception e) {
                Log.e(TAG,"Danger Will Robinson");
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            myBt.cancelDiscovery();
            Log.e(TAG,"stopping discovery");

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.e(TAG,"connecting!");

                mmSocket.connect();
            } catch (IOException connectException) {

                Log.e(TAG,"failed to connect");

                // Unable to connect; close the socket and get out
                try {
                    Log.e(TAG,"close-ah-da-socket");

                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,"failed to close hte socket");

                }
                Log.e(TAG,"returning..");

                return;
            }

            Log.e(TAG,"we can now manage our connection!");

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);

            } catch (IOException e) { }
        }
    }
    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }
    public void manageConnectedSocket(BluetoothSocket mmSocket) {
        ConnectedThread t = new ConnectedThread(mmSocket);
        t.start();
        // manage your socket... I'll probably do a lot of the boiler plate here later
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            firstTextView.setText("Connected Thread");
            secondTextView.setText("Connected");
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    //                  byte[] blah = ("System Time:" +System.currentTimeMillis()).getBytes();
                    //                  write(blah);
                    //                  Thread.sleep(1000);
                    // Read from the InputStream
                    takePicture();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inJustDecodeBounds = false;
                    while (currFile == null) {
                        Thread.sleep(100);
                    }
                    Bitmap bitmap = BitmapFactory.decodeFile(currFile.getAbsolutePath(), options);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    write(byteArray);
                    Thread.sleep(1000);
                    byte[] valBuff = new byte[2];
                    int byt;
                    byt = mmInStream.read(valBuff);
                    int e = valBuff[0];
                    int a = valBuff[1];
                    firstTextView.setText(Integer.toString(e));
                    secondTextView.setText(Integer.toString(a));
                    // Send the obtained bytes to the UI Activity

                    //                  .sendToTarget();
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    //                  break;
                }
            }
        }
        public void connectionLost() {
            Message msg = handle.obtainMessage(CANCEL_CONN);
            //          Bundle bundle = new Bundle();
            //          bundle.putString("NAMES", devs);
            //          msg.setData(bundle);

            handle.sendMessage(msg);

        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //              mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                //              .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}