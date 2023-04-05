/*
Copyright (c) 2022, Nikola Nešković
All rights reserved.

This source code is licensed under the BSD-style license found in the
LICENSE file in the root directory of this source tree.
*/
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