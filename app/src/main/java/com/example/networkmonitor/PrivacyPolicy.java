package com.example.networkmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.networkmonitor.databinding.ActivityPrivacyPolicyBinding;

public class PrivacyPolicy extends AppCompatActivity {

    private ActivityPrivacyPolicyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityPrivacyPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}