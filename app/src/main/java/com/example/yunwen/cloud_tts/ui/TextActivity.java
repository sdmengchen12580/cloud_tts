package com.example.yunwen.cloud_tts.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.yunwen.cloud_tts.R;

public class TextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        findViewById(R.id.buttonPanel).setOnClickListener(v -> startActivity(new Intent(TextActivity.this, MainActivity.class)));
    }
}
