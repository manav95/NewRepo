package transapps.android_bluetooth_host;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.Frame.ROTATE;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import android.os.SystemClock;

public class BluetoothHost extends Activity implements Detector.FaceListener,Detector.ImageListener{

    PhotoDetector detector;
    public static String msgToSend="";
    static int[] measurements = new int[2];
    public static final int STATE_CONNECTION_STARTED = 0;
    public static final int STATE_CONNECTION_LOST = 1;
    public static final int READY_TO_CONN = 2;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // our last connection
    ConnectedThread mConnectedThread;// = new ConnectedThread(socket);
    // track our connections
    ArrayList<ConnectedThread> mConnThreads;
    // bt adapter for all your bt needs (where we get all our bluetooth powers)
    BluetoothAdapter myBt;
    // list of sockets we have running (for multiple connections)
    ArrayList<BluetoothSocket> mSockets = new ArrayList<BluetoothSocket>();
    // list of addresses for devices we've connected to
    ArrayList<String> mDeviceAddresses = new ArrayList<String>();
    // just a name, nothing more...
    String NAME="G6BITCHES";
    // We can handle up to 7 connections... or something...
    UUID[] uuids = new UUID[2];
    // some uuid's we like to use..
    String uuid1 = "05f2934c-1e81-4554-bb08-44aa761afbfb";
    String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";
    // just a tag..
    String TAG = "G6 Bluetooth Host Activity";
    // constant we define and pass to startActForResult (must be >0), that the system passes back to you in your onActivityResult() 
    // implementation as the requestCode parameter.
    int REQUEST_ENABLE_BT = 1;
    AcceptThread accThread;
    TextView connectedDevices;
    Handler handle;
    BroadcastReceiver receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // the activity for this is pretty stripped, just a basic selection ui....
        setContentView(R.layout.activity_main);
        uuids[0] = UUID.fromString(uuid1);
        uuids[1] = UUID.fromString(uuid2);
        connectedDevices = (TextView) findViewById(R.id.connected_devices_values);
        handle = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case STATE_CONNECTION_STARTED:
                        connectedDevices.setText(msg.getData().getString("NAMES"));
                        break;
                    case STATE_CONNECTION_LOST:
                        connectedDevices.setText("");
                        startListening();
                        break;
                    case READY_TO_CONN:
                        startListening();
                    default:
                        break;
                }
            }
        };

        // ....
        myBt = BluetoothAdapter.getDefaultAdapter();
        initializePhotoDetector();
        // run the "go get em" thread..
        accThread = new AcceptThread();
        accThread.start();
    }
    @Override
    public void onFaceDetectionStarted() {
        //
    }
    @Override
    public void onFaceDetectionStopped() {
    }
    @Override
    public void onImageResults(List<Face> faces, Frame image, float timestamp) {
        int e;
        int a;

        if (faces == null) {
            measurements[0]= -1;
            measurements[1]= -1;
        }

        if (faces.size() == 0) {
            //Log.d("TAG", "no face found");
            measurements[0]=0;
            measurements[1]=0;
        }

        //For each face found

        for (int i = 0 ; i < faces.size() ; i++) {
            Face face = faces.get(i);
            //int faceId = face.getId();

            //Appearance
            //Face.GENDER genderValue = face.appearance.getGender();
            //Face.GLASSES glassesValue = face.appearance.getGlasses();


            //Some Expressions
            float engagement = face.emotions.getEngagement();
            float attention = face.expressions.getAttention();

            e=Math.round(engagement);
            a=Math.round(attention);
            measurements[0]=e;
            measurements[1]=a;
        }
    }
    public void initializePhotoDetector() {
        PhotoDetector detector = new PhotoDetector(this);
        detector.setLicensePath("sdk_manavbiodutta@yahoo.com.license");
        detector.setImageListener(this);
        detector.setFaceListener(this);
        detector.setDetectAttention(true);
        detector.setDetectEngagement(true);
        detector.start();
    }
    public void startListening() {
        if(accThread!=null) {
            accThread.cancel();
        }else if (mConnectedThread!= null) {
            mConnectedThread.cancel();
        } else {
            accThread = new AcceptThread();
            accThread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        BluetoothServerSocket tmp;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = myBt.listenUsingInsecureRfcommWithServiceRecord(NAME, uuids[0]);

            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.e(TAG,"Running?");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {

                try {

                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted

                if (socket != null) {
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);

                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);

            } catch (IOException e) { }
        }
    }


    private void manageConnectedSocket(BluetoothSocket socket) {
        // start our connection thread
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        // so the HH can show you it's working and stuff...
        String devs="";
        for(BluetoothSocket sock: mSockets) {
            devs+=sock.getRemoteDevice().getName()+"\n";
        }
        // pass it to the UI....
        Message msg = handle.obtainMessage(STATE_CONNECTION_STARTED);
        Bundle bundle = new Bundle();
        bundle.putString("NAMES", devs);
        msg.setData(bundle);

        handle.sendMessage(msg);
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
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
            byte[] valBuffer = new byte[1000000];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    bytes = mmInStream.read(valBuffer);
                    //Bitmap bmp = BitmapFactory.decodeByteArray(valBuffer, 0, bytes);
                    int width = 640;
                    int height = 480;
                    float timeStamp = (float)SystemClock.elapsedRealtime()/1000f;

                    Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(valBuffer, width, height,
                            Frame.COLOR_FORMAT.YUV_NV21);
                    detector.process(frame);
                    byte[] arr = ByteBuffer.allocate(1).putInt(measurements[0]).array();
                    byte[] secArr = ByteBuffer.allocate(1).putInt(measurements[1]).array();
                    byte[] newM = new byte[2];
                    newM[0] = arr[0];
                    newM[1] = secArr[0];
                    write(newM);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                }
            }
        }
        public void connectionLost() {
            Message msg = handle.obtainMessage(STATE_CONNECTION_LOST);
            handle.sendMessage(msg);
            detector.stop();
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    public static synchronized void setMsg(String newMsg) {
        msgToSend = newMsg;
    }
    public static class HostBroadRec extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b= intent.getExtras();
            String vals ="";
            for(String key: b.keySet()) {
                vals+=key+"&"+b.getString(key)+"Z";
            }
            BluetoothHost.setMsg(vals);
        }
    }
}