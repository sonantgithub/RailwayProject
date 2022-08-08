package com.example.railwayproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private TextView languageCodeTV;
    ImageView micButton;
    TextView editText;
    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    ArrayList<String> data;
    public static final String TAG = "ExtractTextFromAudio";
    Intent speechRecognizerIntent;
    String joined;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initt();
        startFirebaseListenData();

    }

    private void initt() {
        languageCodeTV = findViewById(R.id.idTVDetectedLanguageCode);
        micButton = findViewById(R.id.mic);
        editText = findViewById(R.id.sampleText);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }
//    private void languageDetection(String s) {
//
//        detectLanguage(s);
//    }

//    private void detectLanguage(String string) {
//        // initializing our firebase language detection.
//        FirebaseLanguageIdentification languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
//
//        // adding method to detect language using identify language method.
//        languageIdentifier.identifyLanguage(string).addOnSuccessListener(new OnSuccessListener<String>() {
//            @Override
//            public void onSuccess(String s) {
//                // below line we are setting our
//                // language code to our text view.
//                if (s.equals("und"))
//                {
//                    googleLanguageDetection(s);
//                }
//                else{
//                    languageCodeTV.setText(s);
//                    speechRecognizer.startListening(speechRecognizerIntent);
//                }
//
//
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                // handling error method and displaying a toast message.
//                Toast.makeText(MainActivity.this, "Fail to detect language : \n" + e, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    private void googleLanguageDetection(String s) {
        LanguageIdentifier languageIdentifier =
                LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(s)
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@Nullable String languageCode) {
                                if (languageCode.equals("und")) {
                                    languageCodeTV.setText("undefined");
                                    speechRecognizer.startListening(speechRecognizerIntent);
                                } else {
                                    languageCodeTV.setText(languageCode);
                                    sendTextToUnitySharedFolder();
                                    speechRecognizer.startListening(speechRecognizerIntent);
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be loaded or other internal error.
                                // ...
                            }
                        });
    }

    private void sendTextToUnitySharedFolder() {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "C/mnt/windows/BstSharedFolder");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, "ffileNameee" + ".txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(joined);
            writer.flush();
            writer.close();
            Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //start listing for the firebase flag
    private void startFirebaseListenData() {
        FirebaseDatabase.getInstance("https://signeystreamingdb.firebaseio.com/").getReference().child("flagForRailwayDemo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue().equals("startListening"))
                {
                    speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                        @Override
                        public void onReadyForSpeech(Bundle bundle) {
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            editText.setText("");
                            editText.setHint("Listening...");
                            Log.d(TAG, "onReadyForSpeech: 2");
                        }

                        @Override
                        public void onRmsChanged(float v) {
                            Log.d(TAG, "onReadyForSpeech: 3");
                        }

                        @Override
                        public void onBufferReceived(byte[] bytes) {
                            Log.d(TAG, "onReadyForSpeech: 4");
                        }

                        @Override
                        public void onEndOfSpeech() {
                            //speechRecognizer.startListening(speechRecognizerIntent);
                        }

                        @Override
                        public void onError(int i) {
                            //micButton.setImageResource(R.drawable.ic_baseline_mic_red_24);
                            //speechRecognizer.startListening(speechRecognizerIntent);
                        }

                        @Override
                        public void onResults(Bundle bundle) {
                            micButton.setImageResource(R.drawable.ic_baseline_mic_black_24);
                            data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            joined = TextUtils.join(", ", data);
                            editText.setHint(joined);
                            //googleLanguageDetection(joined);
                            //languageDetection(joined);
                            //sendDataToUnityWithVolley(joined);
                            //sendDataToUnityWithSocket(joined);
                            sendDataToFirebase(joined);
                        }

                        @Override
                        public void onPartialResults(Bundle bundle) {

                        }

                        @Override
                        public void onEvent(int i, Bundle bundle) {

                        }
                    });

//                    micButton.setOnTouchListener((view, motionEvent) -> {
//                        Toast.makeText(this, "N/A", Toast.LENGTH_SHORT).show();
//                        return false;
//                    });
                    //startFirebaseListenData();
                    micButton.setImageResource(R.drawable.ic_baseline_mic_red_24);
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                else if(snapshot.getValue().equals("stopListening")){
                    micButton.setImageResource(R.drawable.ic_baseline_mic_black_24);
                    speechRecognizer.stopListening();
                }
                else{}

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    
    //Sending data to firebase
    private void sendDataToFirebase(String textSendToUnity) {
        FirebaseDatabase.getInstance("https://signeystreamingdb.firebaseio.com/").getReference().child("CurrentChannelName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentChannelValue = String.valueOf(snapshot.getValue());
                HashMap hashMap = new HashMap();
                hashMap.put("framedata",textSendToUnity);
                FirebaseDatabase.getInstance("https://signeystreamingdb.firebaseio.com/").getReference().child("channelsAwaitingResponse").child(currentChannelValue).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        HashMap hashMapTwo = new HashMap();
                        hashMapTwo.put("booleanForSameWord", "true");
                        hashMapTwo.put("isUserStillUsingChannel", "true");
                        FirebaseDatabase.getInstance("https://signeystreamingdb.firebaseio.com/").getReference()
                                .child("channelsAwaitingResponse").child(currentChannelValue).updateChildren(hashMapTwo)
                                .addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                    }
                                });
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }


    //Sending data via volley
    private void sendDataToUnityWithVolley(String textSendToUnity) {
        RequestQueue requestQueue = com.android.volley.toolbox.Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://192.168.1.1:8080", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(MainActivity.this, ""+response.toString(), Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, ""+error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("SendWordToLocalPort", textSendToUnity);
                return params;
            }

        };
        int socketTimeOut = 500000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(stringRequest);
    }

    //Sending data via sockets
    private void sendDataToUnityWithSocket(String textSendToUnity) {
        {
            try {
                mSocket = IO.socket("http://192.168.1.20:3000");
                // option 1 - http://127.0.0.1:8080/

            } catch (URISyntaxException e) {
            }
        }
        mSocket.connect(); // connect the socket
        mSocket.on("respondSendFromAnotherSide", onNewMessage); //Listen response coming from node
        mSocket.emit("androidDataToUnity", textSendToUnity); // send message to the node
    }
    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run1234: " + args[0].toString());
                    Toast.makeText(MainActivity.this, ""+args[0].toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

}

