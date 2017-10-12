package com.example.yunwen.cloud_tts.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.yunwen.cloud_tts.R;
import com.example.yunwen.cloud_tts.constant.Config;
import com.example.yunwen.cloud_tts.utils.Util_Log_Toast;
import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechSynthesizer;
import com.unisound.client.SpeechSynthesizerListener;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


/**
 * 1.开发文档多方案语音识别播报的对象不同
 * 2.方言
 * 3.参数少，无尾断点
 * 4.没找到语音唤醒
 */
// TODO: 2017/10/11  第一次点击edittext没用。
// TODO: 2017/10/11  第一次点击edittext没用。
// TODO: 2017/10/11  第一次点击edittext没用。
// TODO: 2017/10/11  第一次点击edittext没用。
// TODO: 2017/10/11  第一次点击edittext没用。
// TODO: 2017/10/11  第一次点击edittext没用。

public class BaseChatActivity extends NetBaseActivity implements View.OnClickListener{

    private TextView txview_status;
    private EditText et_client_sysout_in;
    private Button bt_send_content;
    private TextView mRecognizerResultText;
    private ProgressBar mVolume;
    private Button stopRecoder;
    private Button stopSpeck;


    //2.用户说的话
    @SuppressWarnings("unused")
    private String mRecognizerText = "";
    private StringBuffer mAsrResultBuffer;


    //3.当前状态
    /**
     * 1.空闲  * 2.识别中  * 3.解析 * 4.播报 * 5.死亡——不播报不识别
     */
    enum AsrStatus {
        idle, recoder, recognizing, speck, dead
    }

    /**
     * 当前状态空闲
     */
    private AsrStatus statue = AsrStatus.speck;


    //4.播报的参数和语音对象
    /**
     * 对应的采样率和说话的语种
     */
    private static int arraySample[] = new int[]{SpeechConstants.ASR_SAMPLING_RATE_BANDWIDTH_AUTO,
            SpeechConstants.ASR_SAMPLING_RATE_16K, SpeechConstants.ASR_SAMPLING_RATE_8K};
    private static String arrayLanguageStr[] = new String[]{SpeechConstants.LANGUAGE_MANDARIN,
            SpeechConstants.LANGUAGE_ENGLISH, SpeechConstants.LANGUAGE_CANTONESE};

    /*** 语音识别*语音合成*/
    private SpeechUnderstander mUnderstander;
    private SpeechSynthesizer mTTSPlayer;


    //5.定时相关
    private Timer timer_twenty_s;
    protected int current_number = 15;


    //6.线程切换状态
    private Thread check_status_is_idel;
    private boolean checking_is_idel = true;
    private Thread check_status_is_speck;
    private boolean checking_is_speck = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideBottomNagative();
        setContentView(R.layout.activity_main);
        /**初始化控件*/
        initView();
        /**初始化语音识别*/
        initRecognizer();
        /**初始化语音合成*/
        initSpecker();
        /**线程一直检测当前是否为空闲状态*/
        check_now_status_is_idel();
        /**线程一直检测当前是否为播报状态——重新计时*/
        check_now_status_is_speck();
    }

    /**
     * 全屏
     */
    private void hideBottomNagative() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * 初始化控件
     */
    private void initView() {
        stopRecoder= (Button) findViewById(R.id.stop_recoder);
        stopSpeck = (Button) findViewById(R.id.stop_speck);
        bt_send_content = (Button) findViewById(R.id.bt_mainsend);
        mVolume= (ProgressBar) findViewById(R.id.volume_progressbar);
        mRecognizerResultText= (TextView) findViewById(R.id.recognizer_result_et);
        txview_status = (TextView) findViewById(R.id.tv_speak);
        et_client_sysout_in = (EditText) findViewById(R.id.et_content);
        stopRecoder.setOnClickListener(this);
        stopSpeck.setOnClickListener(this);
        bt_send_content.setOnClickListener(this);
        txview_status.setOnClickListener(this);
        et_client_sysout_in.setOnTouchListener((v, event) -> {
         if (event.getAction() == MotionEvent.ACTION_UP) {
                current_number = 15;
             if (statue == AsrStatus.idle) {
                 return true;
             } else if(statue == AsrStatus.speck){
                 Util_Log_Toast.log_e(BaseChatActivity.this,"当前正在播报，即将停止播报");
                 mTTSPlayer.stop();
             } else if(statue == AsrStatus.recoder||statue == AsrStatus.recognizing){
                 Util_Log_Toast.log_e(BaseChatActivity.this,"当前正在识别，即将停止播报");
                 mUnderstander.cancel();
             }
             /**点击键盘设置为死亡状态*/
             statue = AsrStatus.dead;
             runOnUiThread(() -> txview_status.setText("点击说话"));
             return false;
         }
            return false;
        });
        et_client_sysout_in.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                current_number = 15;
            }
        });
        timer_twenty_s = new Timer();
        timer_twenty_s.schedule(new TimerTask() {
            @Override
            public void run() {
                current_number -= 1;
                Util_Log_Toast.log_e(BaseChatActivity.this, "" + current_number);
                if (current_number == 0) {
                    Util_Log_Toast.log_e(BaseChatActivity.this, "计时结束");
                    timer_twenty_s.cancel();
                    /**调到下个界面*/
                    startActivity(new Intent(BaseChatActivity.this, TextActivity.class));
                    finish();
                }
            }
        }, 0, 1000);
        mAsrResultBuffer = new StringBuffer();
    }

    /**
     * 点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_speak:
                /**聆听时候，点击没用*/
                if (statue == AsrStatus.speck) {
                    mTTSPlayer.stop();
                    statue = AsrStatus.idle;
                } else if (statue == AsrStatus.dead) {
                    statue = AsrStatus.idle;
                    runOnUiThread(() -> et_client_sysout_in.setText(""));
                }
                break;
            case R.id.bt_mainsend:
                /**edittext为空时候，点击没用*/
                if (et_client_sysout_in.getText().toString().isEmpty()) {
                    return;
                }
                /**不为空直接播报，此时肯定为死亡状态*/
                if (!et_client_sysout_in.getText().toString().isEmpty()) {
                    if(statue == AsrStatus.recoder||statue == AsrStatus.recognizing){
                        mUnderstander.cancel();
                        mVolume.setProgress(0);
                    }
                    mTTSPlayer.playText("" + et_client_sysout_in.getText().toString());
                    runOnUiThread(() -> mRecognizerResultText.setText(et_client_sysout_in.getText().toString()));
                    statue = AsrStatus.speck;
                    runOnUiThread(() -> txview_status.setText("当前正在播报"));
                    /**edittext清空*/
                    runOnUiThread(() -> et_client_sysout_in.setText(""));
                }
                break;
            /**当前正在播报，停止播报*/
            case R.id.stop_speck:
                if (statue == AsrStatus.speck) {
                    mTTSPlayer.stop();
                    mTTSPlayer.init("");
                    statue = AsrStatus.idle;
                }
                break;
            case R.id.stop_recoder:
                if (statue == AsrStatus.recoder) {
                    mUnderstander.cancel();
                    mVolume.setProgress(0);
                    /**点击键盘设置为死亡状态*/
                    statue = AsrStatus.dead;
                    runOnUiThread(() -> txview_status.setText("点击说话"));
                }
                break;
        }
    }

    /**
     * 检测是否为空闲状态
     */
    private void check_now_status_is_idel() {
        check_status_is_idel = new Thread(() -> {
            while (checking_is_idel) {
                if (statue == AsrStatus.idle) {
                    /**清空stringbuffer和2个edittext*/
                    mAsrResultBuffer.delete(0, mAsrResultBuffer.length());
                    runOnUiThread(() -> mRecognizerResultText.setText(""));
                    /**开始识别*/
                    mUnderstander.start();
                    mUnderstander.init("");
                    /**当前正在识别*/
                    statue = AsrStatus.recoder;
                    runOnUiThread(() -> txview_status.setText("当前正在聆听"));
                }
            }
        });
        check_status_is_idel.start();
    }

    /**
     * 线程一直检测当前是否为播报状态——重新计时
     */
    private void check_now_status_is_speck() {
        check_status_is_speck = new Thread(() -> {
            while (checking_is_speck) {
                if (statue == AsrStatus.speck) {
                    current_number = 15;
                }
            }
        });
        check_status_is_speck.start();
    }

    /**
     * 初始化语音识别
     */
    private void initRecognizer() {
        /**创建语音识别对象，appKey和 secret通过 http://dev.hivoice.cn/ 网站申请*/
        mUnderstander = new SpeechUnderstander(this, Config.appKey, Config.secret);
        /**开启可变结果*/
        mUnderstander.setOption(SpeechConstants.ASR_OPT_TEMP_RESULT_ENABLE, true);
        /**设置语义场景*/
        // TODO: 2017/9/28 可以选择
        mUnderstander.setOption(SpeechConstants.NLU_SCENARIO, "videoDefault");
        /**在收到 onRecognizerStart 回调前，录音设备没有打开，请添加界面等待提示*/
        /**修改识别语音*/
        mUnderstander.setOption(SpeechConstants.ASR_SAMPLING_RATE, arraySample[1]);
        mUnderstander.setOption(SpeechConstants.ASR_LANGUAGE, arrayLanguageStr[0]);
        // TODO: 2017/9/28  保存录音数据——数据保存到哪里？
        // TODO: 2017/9/28   recognizer.setRecordingDataEnable(true);
        mUnderstander.setListener(new SpeechUnderstanderListener() {
            @Override
            public void onResult(int type, String jsonResult) {
                switch (type) {
                    /**在线识别*/
                    case SpeechConstants.ASR_RESULT_NET:
                        // 在线识别结果，通常onResult接口多次返回结果，保留识别结果组成完整的识别内容。
                        Util_Log_Toast.log_e("调用在线识别");
                        if (jsonResult.contains("net_asr")
                                && jsonResult.contains("net_nlu")) {
                            try {
                                JSONObject json = new JSONObject(jsonResult);
                                JSONArray jsonArray = json.getJSONArray("net_asr");
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                String status = jsonObject.getString("result_type");
                                Util_Log_Toast.log_e("jsonObject = " + jsonObject.toString());
                                if (status.equals("full")) {
                                    String result = (String) jsonObject
                                            .get("recognition_result");
                                    /**jsonResult有结果，就能解析出识别到用户说的话-并且长度大于1*/
                                    if (jsonResult != null && result.length() > 2) {
                                        /**用户交互了，重新计时*/
                                        current_number = 15;
                                        Util_Log_Toast.log_e(BaseChatActivity.this, "用户说话为：" + result.toString());
                                        /**语音播报语音识别的字符串*/
                                        mTTSPlayer.playText(result.trim());
                                        /**当前正在播报*/
                                        statue = AsrStatus.speck;
                                        runOnUiThread(() -> txview_status.setText("当前正在播报"));
                                    } else if (result.length() == 2) {
                                        Util_Log_Toast.log_e(BaseChatActivity.this, "用户说一个字，不识别");
                                        /**当前为空闲状态*/
                                        statue = AsrStatus.idle;
                                    } else if (result.isEmpty()) {
                                        Util_Log_Toast.log_e(BaseChatActivity.this, "用户没说话");
                                        /**当前为空闲状态*/
                                        statue = AsrStatus.idle;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            //取出语音识别结果
                            asrResultOperate(jsonResult);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onEvent(int type, int timeMs) {
                switch (type) {
                    /**1.用户可以说话了*/
                    case SpeechConstants.ASR_EVENT_RECORDING_START:
                        Util_Log_Toast.log_e(BaseChatActivity.this, "录音设备打开，开始识别，用户可以开始说话");
                        statue = AsrStatus.recoder;
                        break;
                    /**2.说话开始*/
                    case SpeechConstants.ASR_EVENT_SPEECH_DETECTED:
                        Util_Log_Toast.log_e(BaseChatActivity.this, "用户开始说话");
                        break;
                    /**3.音量的改变*/
                    case SpeechConstants.ASR_EVENT_VOLUMECHANGE:
                        // 说话音量实时返回
                        int volume = (Integer) mUnderstander.getOption(SpeechConstants.GENERAL_UPDATE_VOLUME);
//                        if(volume>30){
//                            current_number = 15;
//                        }
                        mVolume.setProgress(volume);
                        break;
                    /**4.识别完*/
                    case SpeechConstants.ASR_EVENT_NET_END:
                        Util_Log_Toast.log_e(BaseChatActivity.this, "识别完成");
                        break;
                    /**5.超时未说话*/
                    case SpeechConstants.ASR_EVENT_VAD_TIMEOUT:
                        Util_Log_Toast.log_e(BaseChatActivity.this, "长时间不说话，但是不语音识别");
//                            mUnderstander.stop();
                        break;
                    /**6.录音停止*/
                    case SpeechConstants.ASR_EVENT_RECORDING_STOP:
                        Util_Log_Toast.log_e(BaseChatActivity.this, "录音停止，即将进入解析用户言语");
                        statue = AsrStatus.recognizing;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int type, String errorMSG) {
                if (errorMSG != null) {
                    /**显示错误信息*/
                    Util_Log_Toast.hitErrorMsg(BaseChatActivity.this, errorMSG);
                } else {
                    if ("".equals(mRecognizerResultText.getText().toString())) {
                        mRecognizerResultText.setText(R.string.no_hear_sound);
                    }
                }
            }
        });
        mUnderstander.init("");
    }

    /**
     * 初始化语音合成
     */
    public void initSpecker() {
        /**创建语音合成（合成就是播报）对象*/
        mTTSPlayer = new SpeechSynthesizer(this, Config.appKey, Config.secret);
        mTTSPlayer.setOption(SpeechConstants.TTS_SERVICE_MODE, SpeechConstants.TTS_SERVICE_MODE_NET);
        /**设置语音合成回调监听*/
        mTTSPlayer.setTTSListener(new SpeechSynthesizerListener() {
            @Override
            public void onEvent(int type) {
                switch (type) {
                    case SpeechConstants.TTS_EVENT_INIT:
                        // 初始化成功回调
                        break;
                    case SpeechConstants.TTS_EVENT_SYNTHESIZER_START:
                        // 开始合成回调
                        break;
                    case SpeechConstants.TTS_EVENT_SYNTHESIZER_END:
                        // 合成结束回调
                        break;
                    case SpeechConstants.TTS_EVENT_BUFFER_BEGIN:
                        // 开始缓存回调
                        break;
                    case SpeechConstants.TTS_EVENT_BUFFER_READY:
                        // 缓存完毕回调
                        break;
                    case SpeechConstants.TTS_EVENT_PLAYING_START:
                        // 开始播放回调
                        break;
                    case SpeechConstants.TTS_EVENT_PLAYING_END:
                        /**播报完成后，由播报转换为识别*/
                        if(statue == AsrStatus.speck){
                            statue = AsrStatus.idle;
                            current_number = 15;
                        }else if(statue == AsrStatus.dead){
                            /**死亡状态时候，不做操作*/
                        }
                        // 播放完成回调
                        break;
                    case SpeechConstants.TTS_EVENT_PAUSE:
                        // 暂停回调
                        break;
                    case SpeechConstants.TTS_EVENT_RESUME:
                        // 恢复回调
                        break;
                    case SpeechConstants.TTS_EVENT_STOP:
                        // 停止回调
                        break;
                    case SpeechConstants.TTS_EVENT_RELEASE:
                        // 释放资源回调
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int type, String errorMSG) {
                /**吐司出错*/
                Util_Log_Toast.hitErrorMsg(BaseChatActivity.this, errorMSG);
            }
        });
        // TODO: 2017/10/12 不知道是否需要加上此句话 
        mTTSPlayer.init("");
        /**初始化语音合成*/
        mTTSPlayer.playText("你好啊，欢饮光临。");
        /**当前正在播报*/
        statue = AsrStatus.speck;
        runOnUiThread(() -> txview_status.setText("当前正在播报"));
    }

    /**
     * 释放资源
     */
    @Override
    protected void onStop() {
        super.onStop();
        statue = AsrStatus.speck;
        timer_twenty_s = null;
        mRecognizerText = null;
        mAsrResultBuffer = null;

        if (mUnderstander != null) {
            mUnderstander.stop();
        }
        if (mTTSPlayer != null) {
            mTTSPlayer.stop();
        }
        //关闭线程
        checking_is_idel = false;
        if (check_status_is_idel != null) {
            check_status_is_idel.interrupt();
            check_status_is_idel = null;
        }
        checking_is_speck = false;
        if (check_status_is_speck != null) {
            check_status_is_speck.interrupt();
            check_status_is_speck = null;
        }
    }

    /**
     * 语音解析工具方法
     */
    private void asrResultOperate(String jsonResult) {
        JSONObject asrJson;
        try {
            asrJson = new JSONObject(jsonResult);
            JSONArray asrJsonArray = asrJson.getJSONArray("net_asr");
            JSONObject asrJsonObject = asrJsonArray.getJSONObject(0);
            String asrJsonStatus = asrJsonObject.getString("result_type");
            /**清空数据*/
            mRecognizerResultText.setText("");
//            if (asrJsonStatus.equals("change")) {
//                mRecognizerResultText.append(mAsrResultBuffer.toString());
//                mRecognizerResultText.append(asrJsonObject.getString("recognition_result"));
//            } else {
//                mAsrResultBuffer.append(asrJsonObject.getString("recognition_result"));
//                mRecognizerResultText.append(mAsrResultBuffer.toString());
//            }
            if (!asrJsonStatus.equals("change")) {
                mAsrResultBuffer.append(asrJsonObject.getString("recognition_result"));
                Util_Log_Toast.log_e(BaseChatActivity.this, asrJsonObject.getString("recognition_result") + "");
                mRecognizerResultText.append(mAsrResultBuffer.toString());
                Util_Log_Toast.log_e(BaseChatActivity.this, mAsrResultBuffer.toString() + "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击屏幕，重新计时
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            current_number = 15;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 监听软键盘的点击事件
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        for (int i = 0; i < 280; i++) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager.isActive()) {
                    inputMethodManager.hideSoftInputFromWindow(BaseChatActivity.this.getCurrentFocus().getWindowToken(), 0);
                    current_number = 15;
                    return true;
                }
            } else if (event.getKeyCode() == i) {
                current_number = 15;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
