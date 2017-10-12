package com.example.yunwen.cloud_tts.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.yunwen.cloud_tts.baserxjava.Base_Http_Retrofit;
import com.example.yunwen.cloud_tts.constant.Config;
import com.example.yunwen.cloud_tts.entity.Access_Token;
import com.example.yunwen.cloud_tts.entity.KeeyRobatOnlion;
import com.example.yunwen.cloud_tts.utils.Util_Log_Toast;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NetBaseActivity extends AppCompatActivity {
    /**是否有网*/
    public boolean ISNETAVAILABLE;
    private IntentFilter intentFilter;
    private NetworkChangeReceiver networkChangeReceiver;


    public static String ACCESS_TOKEN = null;
    public Subscription access_token_retrofit;
    public Subscription robarOnlion_retrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**监听网络广播*/
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    /**监听网络广播*/
    class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                /**有网*/
                ISNETAVAILABLE = true;
                /**获取accesstoken*/
                getAccess_Token(Config.APPID, Config.SECRET);
            } else {
                ISNETAVAILABLE = false;
            }
        }
    }

    /**获取ACCESS_TOKEN网络请求封装*/
    private void getAccess_Token(String appId, String secret) {
        access_token_retrofit = Base_Http_Retrofit.getInstance()
                .geterver()
                .getAccess_Token(appId, secret)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Access_Token>() {
                    @Override
                    public void onCompleted() {
                        Util_Log_Toast.log_e(NetBaseActivity.this,"获取accesstoken值完成，准备设置机器人在线");
                        setRobatOnlioning();
                    }
                    @Override
                    public void onError(Throwable e) {
                        Util_Log_Toast.log_e(NetBaseActivity.this,"获取accesstoken值失败");
                    }
                    @Override
                    public void onNext(Access_Token access_token) {
                        if (null != access_token.getAccess_token()) {
                            ACCESS_TOKEN = access_token.getAccess_token();
                            Util_Log_Toast.log_e(NetBaseActivity.this,"获取accesstoken值为："+ACCESS_TOKEN);
                            SharedPreferences leave_token = getSharedPreferences("leave_token", MODE_PRIVATE);
                            SharedPreferences.Editor edit = leave_token.edit();
                            edit.putString("token", ACCESS_TOKEN);
                            edit.apply();
                        } else {
                            Util_Log_Toast.log_e(NetBaseActivity.this,"获取accesstoken值为空");
                            SharedPreferences leave_token = getSharedPreferences("leave_token", MODE_PRIVATE);
                            ACCESS_TOKEN = leave_token.getString("token", "");
                        }
                    }
                });
    }

    /**请求机器人长连接网络请求封装*/
    private void setRobatOnlioning() {
        robarOnlion_retrofit = Base_Http_Retrofit.getInstance()
                .geterver()
                .setRobotOnlioning(ACCESS_TOKEN, Config.ROBAT_ONLIONING, "", Config.CLIENDID, Config.SOURCEID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<KeeyRobatOnlion>() {
                    @Override
                    public void onCompleted() {
                        Util_Log_Toast.log_e(NetBaseActivity.this,"机器人在线请求成功");
                        ACCESS_TOKEN = null;
                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onNext(KeeyRobatOnlion keeyRobatOnlion) {
                    }
                });
    }

    /**销毁（广播和）*/
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkChangeReceiver);
        if (null != access_token_retrofit) {
            access_token_retrofit.unsubscribe();
            access_token_retrofit = null;
        }
        if(null!=robarOnlion_retrofit){
            robarOnlion_retrofit.unsubscribe();
            robarOnlion_retrofit = null;
        }
    }
}
