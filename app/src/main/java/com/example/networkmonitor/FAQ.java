package com.example.networkmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.networkmonitor.databinding.ActivityFaqBinding;

public class FAQ extends AppCompatActivity {

    ActivityFaqBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityFaqBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}