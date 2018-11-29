/*
 * Copyright 2015-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.demo.androidpubsub;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

/*
 * This is the beginning screen that lets the user select if they want to upload or download
 */
public class ImageViewActivity extends Activity {
    private static final String TAG = ImageViewActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        initUI();
        imageView_JPG();
    }

    private void initUI() {

    }

    private void imageView_JPG()
    {
        String filename = "/storage/emulated/0/Desert.jpg";
        filename = SettingData.getSharedPreferenceString(getApplicationContext(), SettingData.PREF_AWS_S3_IMAGE_FILE);
        Log.d(TAG, String.format("image file: %s", filename));

        File imgFile = new File(filename);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imageView1);
            myImage.setImageBitmap(myBitmap);
        } else {
            String msg = String.format("no found file: %s", filename);
            Log.d(TAG, msg);
        }
    }
}
