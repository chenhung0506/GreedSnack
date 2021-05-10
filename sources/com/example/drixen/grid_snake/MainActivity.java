package com.example.drixen.grid_snake;

import android.os.Bundle;
import android.support.p003v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((View) new MainBoard(this));
    }
}
