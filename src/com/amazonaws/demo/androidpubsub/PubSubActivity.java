/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsub;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected;

public class PubSubActivity extends Activity implements View.OnClickListener {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1rsxf9l9806mh-ats.iot.ap-northeast-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    // private static final String COGNITO_POOL_ID = "ap-northeast-2_BPW5aWbOU";
    private static final String COGNITO_POOL_ID = "ap-northeast-2:340c8df9-7178-4f78-b8d0-edcad9bf6b95";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "tizen-policy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.AP_NORTHEAST_2; //Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    EditText txtSubcribe;
    EditText txtTopic;
    EditText txtMessage;

    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnMenuCamera;
    Button btnMenuSetting;
    Button btnDoorOpen;
    LinearLayout layoutMenuCamera;
    LinearLayout layoutMenuSetting;

    Button btnConnect;
    Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    private static Context context = null;
    String subscribe_token = null;
    String topic_token = null;

    // s3 download
    private Button btnDownload;
    private Button btnViewImage;

    private static final int DOWNLOAD_SELECTION_REQUEST_CODE = 1;

    // This is the main class for interacting with the Transfer Manager
    static TransferUtility transferUtility;

    // The SimpleAdapter adapts the data about transfers to rows in the UI
    static SimpleAdapter simpleAdapter;

    // A List of all transfers
    static List<TransferObserver> observers;

    /**
     * This map is used to provide data to the SimpleAdapter above. See the
     * fillMap() function for how it relates observers to rows in the displayed
     * activity.
     */
    static ArrayList<HashMap<String, Object>> transferRecordMaps;
    static int checkedIndex;
    static Util util;

    static boolean door_opened = false;  // false : door close, true : door open
    static private Handler mHandler = null;
    static public ProgressPopupDialog mProgressPopupDialog = null;

    private void handleMessage_Action(Message msg) {
        Intent intent = null;
        String result = null;
        String tstr = null;
        Log.d(LOG_TAG, "handleMessage: " + msg.what);

        switch (msg.what) {
            case SettingData.MESSAGE_CONNECTED:
                result = (String)msg.obj;
                SendMessage(SettingData.MESSAGE_TOAST, result);
                if (result.contains("Connect") == true)
                    onClickSubscribe();
                break;
            case SettingData.MESSAGE_SUBSCRIBE:
                result = (String)msg.obj;
                SendMessage(SettingData.MESSAGE_TOAST, "AWS Message arrived: " + result);
                SendMessage(SettingData.MESSAGE_POPUP_DIALOG, getResources().getString(R.string.progress_downloading));
                beginDownload(result);
                break;
            case SettingData.MESSAGE_DOWNLOAD:
                result = (String)msg.obj;
                SendMessage(SettingData.MESSAGE_TOAST, "AWS S3: " + result);
                if (mProgressPopupDialog != null) {
                    mProgressPopupDialog.dialogDismiss(); // 없애기
                    mProgressPopupDialog = null;
                }
                imageView();
                break;
            case SettingData.MESSAGE_POPUP_DIALOG:
                if (mProgressPopupDialog != null) {
                    mProgressPopupDialog.dialogDismiss(); // 없애기
                    mProgressPopupDialog = null;
                }
                result = (String)msg.obj;
                Log.i(LOG_TAG,"MESSAGE_PROGRESS_POPUP result: " + result + ", mProgressPopupDialog: " + mProgressPopupDialog);
                if (mProgressPopupDialog == null) {
                    String error_msg = getResources().getString(R.string.popup_download_fail);
                    mProgressPopupDialog = new ProgressPopupDialog(this, result, error_msg);
                    mProgressPopupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    mProgressPopupDialog.show();
                }

                break;
            case SettingData.MESSAGE_RESULT_DIALOG:
                break;
            case SettingData.MESSAGE_TOAST:
                result = (String)msg.obj;
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                handleMessage_Action(msg);
            }
        };

        this.context = getApplicationContext();
        verifyStoragePermissions(this);

        setLayout(savedInstanceState);
        setLayoutS3(savedInstanceState);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    btnConnect.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnConnect.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }

        btnConnect.callOnClick();
    }

    private void setLayout(Bundle savedInstanceState) {
        // Sets up UI references.
        btnMenuCamera = (Button) findViewById(R.id.btnMenuCamera);
        btnMenuCamera.setOnClickListener(this);
        btnMenuSetting = (Button) findViewById(R.id.btnMenuSetting);
        btnMenuSetting.setOnClickListener(this);
        btnDoorOpen = (Button) findViewById(R.id.btnDoorOpen);
        btnDoorOpen.setOnClickListener(this);

        layoutMenuCamera = (LinearLayout) findViewById(R.id.layoutMenuCamera);
        layoutMenuSetting = (LinearLayout) findViewById(R.id.layoutMenuSetting);

        txtSubcribe = (EditText) findViewById(R.id.txtSubcribe);
        txtTopic = (EditText) findViewById(R.id.txtTopic);
        txtMessage = (EditText) findViewById(R.id.txtMessage);

        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
        btnSubscribe.setOnClickListener(subscribeClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);

        subscribe_token = SettingData.getSharedPreferenceString(context, SettingData.PREF_AWS_SUBSCRIBE_TOKEN);
        topic_token = SettingData.getSharedPreferenceString(context, SettingData.PREF_AWS_TOPIC_TOKEN);
        if (subscribe_token == "") {
            subscribe_token = SettingData.DEFAULT_AWS_SUBSCRIBE_NAME;
        }
        if (topic_token == "") {
            topic_token = SettingData.DEFAULT_AWS_TOPIC_NAME;
        }
        txtSubcribe.setText(subscribe_token);
        txtTopic.setText(topic_token);

    }

    protected void setLayoutS3(Bundle savedInstanceState)
    {
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnViewImage = (Button) findViewById(R.id.buttonViewImage);
        // Launches an activity for the user to select an object in their S3
        // bucket to download
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PubSubActivity.this, DownloadActivity.class);
                startActivity(intent);
            }
        });

        btnViewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(PubSubActivity.this, ImageViewActivity.class);
                startActivity(intent);
            }
        });

        // Initializes TransferUtility, always do this before using it.
        util = new Util();
        transferUtility = util.getTransferUtility(this);
    }

    private void doorHoverOpenDraw(Boolean open) {
        if (open == true)
            btnDoorOpen.setBackgroundResource(R.drawable.doorlock_unlock_hover);
        else
            btnDoorOpen.setBackgroundResource(R.drawable.doorlock_lock_hover);
    }

    private void doorActiveOpenDraw(Boolean open) {
        if (open == true)
            btnDoorOpen.setBackgroundResource(R.drawable.doorlock_unlock_active);
        else
            btnDoorOpen.setBackgroundResource(R.drawable.doorlock_lock_active);
    }

    @Override
    public void onClick(View v) {

        final String topic = txtTopic.getText().toString();
        String msg = null;

        switch(v.getId()) {
            case R.id.btnMenuCamera:
                layoutMenuCamera.setVisibility(View.VISIBLE);
                layoutMenuSetting.setVisibility(View.GONE);
                btnMenuCamera.setBackgroundResource(R.drawable.doorlock_pic_active);
                //btnDoorOpen.setBackgroundResource(R.drawable.doorlock_lock_hover);
                btnMenuSetting.setBackgroundResource(R.drawable.doorlock_set_hover);
                doorHoverOpenDraw(door_opened);
                break;
            case R.id.btnMenuSetting:
                layoutMenuCamera.setVisibility(View.GONE);
                layoutMenuSetting.setVisibility(View.VISIBLE);
                btnMenuCamera.setBackgroundResource(R.drawable.doorlock_pic_hover);
                //btnDoorOpen.setBackgroundResource(R.drawable.doorlock_lock_hover);
                btnMenuSetting.setBackgroundResource(R.drawable.doorlock_set_active);
                doorHoverOpenDraw(door_opened);
                break;
            case R.id.btnDoorOpen:
                btnMenuCamera.setBackgroundResource(R.drawable.doorlock_pic_hover);
                btnMenuSetting.setBackgroundResource(R.drawable.doorlock_set_hover);
                if (!door_opened) {
                    msg = getResources().getString(R.string.message_door_open);
                    door_opened = true;
                } else {
                    msg = getResources().getString(R.string.message_door_close);
                    door_opened = false;
                }
                doorActiveOpenDraw(door_opened);
                break;
        }
        if (msg != null) {
            try {
                mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
                tvLastMessage.setText(msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Message send error.", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear transfer listeners to prevent memory leak, or
        // else this activity won't be garbage collected.
        if (observers != null && !observers.isEmpty()) {
            for (TransferObserver observer : observers) {
                observer.cleanTransferListener();
            }
        }
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                            final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == Connected) {
                                    tvStatus.setText("Connected");
                                    SendMessage(SettingData.MESSAGE_CONNECTED, "AWS PUB: Connected");

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        }
    };

    View.OnClickListener subscribeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onClickSubscribe();
        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onClickPublish();
        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };

    private void onClickSubscribe() {
        final String topic = txtSubcribe.getText().toString();

        Log.d(LOG_TAG, "topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        Log.d(LOG_TAG, "Message arrived:");
                                        Log.d(LOG_TAG, "   Topic: " + topic);
                                        Log.d(LOG_TAG, " Message: " + message);

                                        tvLastMessage.setText(message);
                                        SendMessage(SettingData.MESSAGE_SUBSCRIBE, message);

                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(LOG_TAG, "Message encoding error.", e);
                                    }
                                }
                            });
                        }
                    });
            if (subscribe_token.equals(topic) == false) {
                subscribe_token = topic;
                SettingData.setSharedPreferenceString(context, SettingData.PREF_AWS_SUBSCRIBE_TOKEN, subscribe_token);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    private void onClickPublish() {
        final String topic = txtTopic.getText().toString();
        final String msg = txtMessage.getText().toString();

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            if (topic_token.equals(topic) == false) {
                topic_token = topic;
                SettingData.setSharedPreferenceString(context, SettingData.PREF_AWS_TOPIC_TOKEN, topic_token);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, String.format("onActivityResult requestCode: %d, resultCode: %d", requestCode, resultCode));
        if (requestCode == DOWNLOAD_SELECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Start downloading with the key they selected in the
                // DownloadSelectionActivity screen.
                String key = data.getStringExtra("key");
                beginDownload(key);
            }
        }
    }

    /*
     * Begins to download the file specified by the key in the bucket.
     */
    private void beginDownload(String key) {
        // Location to download files from S3 to. You can choose any accessible
        // file.
        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + key);
        Log.d(LOG_TAG, "beginDownload file: " + file.toString());
        SettingData.setSharedPreferenceString(getApplicationContext(), SettingData.PREF_AWS_S3_IMAGE_FILE, file.toString());

        // Initiate the download
        TransferObserver observer = transferUtility.download(Constants.BUCKET_NAME, key, file);

        /*
         * Note that usually we set the transfer listener after initializing the
         * transfer. However it isn't required in this sample app. The flow is
         * click upload button -> start an activity for image selection
         * startActivityForResult -> onActivityResult -> beginUpload -> onResume
         * -> set listeners to in progress transfers.
         */
        observer.setTransferListener(new DownloadListener());
    }

    /*
     * A TransferListener class that can listen to a download task and be
     * notified when the status changes.
     */
    private static class DownloadListener implements TransferListener, Serializable {
        // Simply updates the list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(LOG_TAG, "onError: " + id, e);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(LOG_TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.d(LOG_TAG, "onStateChanged: " + id + ", " + state);
            if (state == TransferState.COMPLETED) {
                SendMessage(SettingData.MESSAGE_DOWNLOAD, TransferState.COMPLETED.toString());
            }
        }
    }

    public static void SendMessage(int id, String result)
    {
        if (mHandler == null) return;
        Message message = mHandler.obtainMessage();
        message.what = id;
        message.arg1 = id;
        message.obj = result;
        mHandler.sendMessage(message);
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            activity.requestPermissions(
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void imageView()
    {
        String filename = SettingData.getSharedPreferenceString(getApplicationContext(), SettingData.PREF_AWS_S3_IMAGE_FILE);
        Log.d(LOG_TAG, String.format("image file: %s", filename));

        File imgFile = new  File(filename);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imageView1);
            myImage.setImageBitmap(myBitmap);
        } else {
            String msg = String.format("no found file: %s", filename);
            Log.d(LOG_TAG, msg);
        }
    }
}
