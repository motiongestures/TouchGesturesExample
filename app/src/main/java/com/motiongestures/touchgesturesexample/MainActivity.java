package com.motiongestures.touchgesturesexample;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.motiongestures.touchgesturesexample.events.TestGestureDrawingFinishedEvent;
import com.motiongestures.touchgesturesexample.events.TestGestureDrawingFinishedListener;
import com.motiongestures.touchgesturesexample.events.TouchGesturePoint;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import GREProtocol.Greapi;

import static android.Manifest.permission.INTERNET;

public class MainActivity extends AppCompatActivity implements TestGestureDrawingFinishedListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_INTERNET = 0;

    private ArrayAdapter<String> gesturesListAdapter = null;
    private ListView recognizedGesturesList;
    private List<Greapi.Point> lastReceivedPoints = null;

    private SocketAdapter socketAdapter = new SocketAdapter();
    private WebSocket webSocket;
    private String currentSessionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //set ourselves as the drawing finished listener
        TestTouchGestureView touchGestureView = findViewById(R.id.touchView);
        touchGestureView.setListener(this);

        gesturesListAdapter = new ArrayAdapter<>(this,R.layout.gesture_item);
        recognizedGesturesList = findViewById(R.id.touchRecognizedGesturesList);
        recognizedGesturesList.setAdapter(gesturesListAdapter);


        mayConnectToInternet();
    }

    @Override
    public void drawingFinished(TestGestureDrawingFinishedEvent event) {
        lastReceivedPoints = new ArrayList<>(event.getPoints().size());
        for(TouchGesturePoint point : event.getPoints()) {
            lastReceivedPoints.add(Greapi.Point.newBuilder().setX(point.getX()).setY(point.getY()).build());
        }
        connect();
    }

    private void disconnect() {
        if(webSocket != null) {
            webSocket.removeListener(socketAdapter);
            webSocket.disconnect();
        }
    }

    private void connect() {
        try {
            //change URI to match your project

            webSocket = new WebSocketFactory().createSocket("wss://sdk.motiongestures.com/recognition?api_key=<replace key>");
            webSocket.addListener(socketAdapter);
            currentSessionId = UUID.randomUUID().toString();
            webSocket.connectAsynchronously();
        } catch (IOException e) {
            Log.e(TAG, "Cannot create socket connection", e);
        }
    }

    @Override
    protected void onPause() {
       disconnect();
       super.onPause();
    }

    private boolean mayConnectToInternet() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG,"We can connect to the internet");
            return true;
        }
        if (checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"We can connect to the internet");
            return true;
        }
        requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
        Log.d(TAG,"Cannot connect to the internet");
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_INTERNET) {
            boolean enabled = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if(!enabled) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(R.string.no_internet_dialog_title);
                alertDialog.setMessage(getString(R.string.no_internet_dialog_message));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
    }

    private void sendLatestSamples() throws IOException {
        Greapi.Touch touchMessage = Greapi.Touch.newBuilder()
                .addAllPoints(lastReceivedPoints)
                .build();
        Greapi.RecognitionRequest recognition = Greapi.RecognitionRequest.newBuilder()
                .setId(currentSessionId)
                //IMPORTANT: set the request type to touch request
                .setRequestType(Greapi.RequestType.TouchRequest)
                .setTouch(touchMessage)
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        recognition.writeTo(outputStream);
        webSocket.sendBinary(outputStream.toByteArray());
    }

    private final class SocketAdapter extends WebSocketAdapter {
        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            try{
                ByteArrayInputStream inputStream = new ByteArrayInputStream(binary);
                final Greapi.RecognitionResponse recognitionResponse = Greapi.RecognitionResponse.parseFrom(inputStream);
                if(recognitionResponse.getStatus() == Greapi.Status.GestureEnd) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int size = Math.min(recognitionResponse.getNamesCount(),recognitionResponse.getLabelsCount());
                            for(int i =0;i<size;i++) {
                                gesturesListAdapter.add("Recognized gesture " + recognitionResponse.getNames(i) + " with label " + recognitionResponse.getLabels(i));
                            }
                        }
                    });
                } else {
                    Log.d(TAG,"Received recognition response with status "+recognitionResponse.getStatus());
                }
            }catch(IOException ex) {
                Log.e(TAG,"Error deserializing the recognition response",ex);
            }
            disconnect();
        }
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            Log.d(TAG,"Connected to server");
            sendLatestSamples();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            Log.e(TAG,"Cannot connect");
            super.onConnectError(websocket, exception);
        }
    }

}
