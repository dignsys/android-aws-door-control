/*
 * Copyright (c) 2018 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsub;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressPopupDialog extends Dialog {
    private static final String TAG = ProgressPopupDialog.class.getSimpleName();

    private TextView txtProgressName;
    private TextView txtProgress;
    private String result_error;
    private ProgressBar progressBar;
    private Boolean mPolling = false;
    private int pStep = 10;
    private int pStatus = 0;
    private int progressMax = 0;
    private Handler handler = new Handler();

    public ProgressPopupDialog(final Context context, String name, String error) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.active_circle_progressbar);
        result_error = error;

        //String strName = context.getResources().getString(R.string.progress_ble_paring);
        progressMax = (10000 / pStep);

        txtProgressName = (TextView) findViewById(R.id.txtProgressName);
        txtProgress = (TextView) findViewById(R.id.txtProgress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        updateProgressName(name);

        progressBar.setOnClickListener(new ProgressBar.OnClickListener() {
            @Override public void onClick(View view) {
            //reset_resource();
            //dismiss();
            pStatus = progressMax + 1;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
            while (pStatus <= progressMax) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int pStatus1 = (pStatus * 10) / 1000;
                        progressBar.setProgress(pStatus / 10);
                        txtProgress.setText(pStatus1 + " sec");
                    }
                });
                try {
                    Thread.sleep(pStep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pStatus++;
            }
            reset_resource();
            dismiss();
            }
        }).start();
    }

    private void reset_resource()
    {
        try {
            Log.i(TAG,"@@@ reset_resource pStatus: " + pStatus);
            PubSubActivity.mProgressPopupDialog = null;
            if ((pStatus - 1) == progressMax) {
                PubSubActivity.SendMessage(SettingData.MESSAGE_RESULT_DIALOG, result_error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dialogDismiss() {
        progressBar.callOnClick();
    }

    public void updateProgressName(String progressName) {
        Log.i(TAG, "progressName: " + progressName);
        txtProgressName.setText(progressName);
        restartProgress();
    }

    public void restartProgress() {
        pStatus = 0;
    }
}
