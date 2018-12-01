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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SettingData {
    private final static String TAG = SettingData.class.getSimpleName();

    //message hander
    public static final int MESSAGE_CONNECTED = 1;
    public static final int MESSAGE_SUBSCRIBE = 2;
    public static final int MESSAGE_DOWNLOAD = 3;
    public static final int MESSAGE_POPUP_DIALOG = 4;
    public static final int MESSAGE_RESULT_DIALOG = 5;
    public static final int MESSAGE_TOAST = 100;

    //setting Preference
    public static final String PrefActivity = "com.amazonaws.demo.androidsubpub.settingdata";
    public static final String PREF_AWS_SUBSCRIBE_TOKEN = "aws_subcsribe_token";
    public static final String PREF_AWS_TOPIC_TOKEN = "aws_topic_token";
    public static final String PREF_AWS_S3_IMAGE_FILE = "aws_s3_imagefile_token";
    public static final int TRUE  = 1;
    public static final int FALSE = 0;

    // PubSub
    public static final String DEFAULT_AWS_SUBSCRIBE_NAME = "tizen/notify";
    public static final String DEFAULT_AWS_TOPIC_NAME = "tizen/cmd";

    public static void setSharedPreferenceInt(Context context, String name, int mode){
        try {
            SharedPreferences pref = context.getSharedPreferences(SettingData.PrefActivity, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(name, mode);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getSharedPreferenceInt(Context context, String name){
        int ret = 0;
        try {
            SharedPreferences pref = context.getSharedPreferences(SettingData.PrefActivity, Activity.MODE_PRIVATE);
            ret = pref.getInt(name, FALSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void setSharedPreferenceString(Context context, String name, String mode){
        try {
            SharedPreferences pref = context.getSharedPreferences(SettingData.PrefActivity, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(name, mode);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSharedPreferenceString(Context context, String name){
        String ret = "";
        try {
            SharedPreferences pref = context.getSharedPreferences(SettingData.PrefActivity, Activity.MODE_PRIVATE);
            ret = pref.getString(name, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
