package com.example.networkmonitor;
//nazovi je my internet tracker lite

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.networkmonitor.databinding.ActivityMainBinding;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long MILLION=1048576;
    private static final long SECOND=1000;
    private static final int THOUSAND =1000;
    private static final int BYTES_IN_MB =1024*1024;
    private static final int BYTES_IN_KB =1024;
    private static final int MIN_DATA_AMOUNT_IN_MB =1;

    private ActivityMainBinding binding;
    private boolean notificationTracker;
    private List<String> monthlyUsage;
    private List<String> dailyUsage;
    private static final int PERMISSION_CODE=1;
    private Intent intentService;
    private boolean loaded=false;
    String[] permissions={Manifest.permission.INTERNET};

    enum NetworkType{WIFI,MOBILE}
    enum DATERV{DAY,MONTH} //date recycler view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notificationTracker=true;

        binding.card1.setVisibility(View.VISIBLE);
        binding.card2.setVisibility(View.GONE);
        binding.card3.setVisibility(View.GONE);

        binding.switchTracker.performClick();
        monthlyUsage=new ArrayList<>();
        dailyUsage=new ArrayList<>();

        intentService=new Intent(this,NotificationService.class);
        startForegroundService(intentService);
        settingsWorkingThread();

        requestPermissions();
        setListeners();
        updateUI();
        recyclerViewWorkingThread(NetworkType.WIFI);
    }
    @Override
    protected void onStart(){
        super.onStart();
        if(getGrantStatus()){
            Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }
    @Override

    protected void onResume() {
        super.onResume();
        if (!getGrantStatus()) {
            Toast.makeText(this,"Permission lost",Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissions(){
        if (!checkPermissions(MainActivity.this,permissions)){
            ActivityCompat.requestPermissions(this,permissions,PERMISSION_CODE);
        }
    }


    private boolean checkPermissions(Context context,String[] permissions){

        for(String permission:permissions){
            if(ActivityCompat.checkSelfPermission(context,permission)!=PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,"Internet and state permissions denied",Toast.LENGTH_SHORT).show();
                }
        }
    }

    private boolean getGrantStatus(){
        AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            return(getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return mode ==MODE_ALLOWED;
        }
    }

    private void updateUI(){

        long usageAtStart=TrafficStats.getTotalRxBytes();

        Runnable monitor=new Runnable() {
            final Handler handler=new Handler();
            @SuppressWarnings("BusyWait")
            @Override
            public void run() {
                while(!Thread.interrupted()) {
                    double tempDown = ((double) TrafficStats.getTotalRxBytes());
                    double tempUp= ((double)TrafficStats.getTotalTxBytes());
                    double total =  ((double)TrafficStats.getTotalRxBytes() - usageAtStart);

                    try {
                        Thread.sleep(SECOND);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    double currDown=(double)TrafficStats.getTotalRxBytes()-tempDown;
                    double currUp=(double)TrafficStats.getTotalTxBytes()-tempUp;

                    String download=roundingSpeed(currDown);
                    String upload=roundingSpeed(currUp);

                    handler.post(()-> {
                        DecimalFormat df = new DecimalFormat("0.00");
                        String totalString =df.format(total / MILLION) + "MB";
                        binding.total.setText(totalString);
                        binding.currentDown.setText(download);
                        binding.currentUp.setText(upload);

                    });
                }
            }
        };
        Thread start=new Thread(monitor);
        start.start();
    }

    private String roundingSpeed(double value) {

        DecimalFormat df = new DecimalFormat("0.00");

        if (value>MILLION){
            return df.format(value/BYTES_IN_MB)+"MB/s";
        }
        if (value>THOUSAND){
            return df.format(value/BYTES_IN_KB)+"KB/s";
        }
        if (value==0)
            return 0+"B/s";
        return df.format(value)+"B/s";

    }

    @SuppressLint({"NonConstantResourceId", "ClickableViewAccessibility"})
    private void setListeners() {
        binding.navbar.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.monitor:
                    binding.card1.setVisibility(View.VISIBLE);
                    binding.card2.setVisibility(View.GONE);
                    binding.card3.setVisibility(View.GONE);
                    return true;

                case R.id.usage:
                    if (loaded) {
                        binding.card1.setVisibility(View.GONE);
                        binding.card2.setVisibility(View.VISIBLE);
                        binding.card3.setVisibility(View.GONE);
                        return true;
                    }
                case R.id.settings:
                    binding.card1.setVisibility(View.GONE);
                    binding.card2.setVisibility(View.GONE);
                    binding.card3.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });

        binding.switchTracker.setOnClickListener(v -> switchOnClick());

        binding.constraintLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                if (binding.card1.getVisibility() == View.VISIBLE) {
                    binding.navbar.setSelectedItemId(R.id.settings);
                    return;
                }

                if (binding.card2.getVisibility() == View.VISIBLE) {
                    binding.navbar.setSelectedItemId(R.id.usage);
                }
            }

            @Override
            public void onSwipeRight() {
                if (binding.card2.getVisibility() == View.VISIBLE) {
                    binding.navbar.setSelectedItemId(R.id.monitor);
                    return;
                }
                if (binding.card3.getVisibility() == View.VISIBLE) {
                    binding.navbar.setSelectedItemId(R.id.settings);
                }
            }
        });

        binding.radioWifi.setOnClickListener(view -> {
            recyclerViewLoading(NetworkType.WIFI,DATERV.DAY);
        });

        binding.radioMobile.setOnClickListener(view -> {
            recyclerViewLoading(NetworkType.MOBILE,DATERV.MONTH);
        });

        binding.radioToday.setOnClickListener(view -> {
            recyclerViewLoading(NetworkType.WIFI,DATERV.DAY);
        });
        binding.radioThisMonth.setOnClickListener(view -> {
            recyclerViewLoading(NetworkType.WIFI,DATERV.MONTH);
        });

    }

//    private void getPing(){
//        String str = "";
//        try {
//            java.lang.Process process = Runtime.getRuntime().exec(
//                    "/system/bin/ping -c 8 " + "www.google.com");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    process.getInputStream()));
//            int i;
//            char[] buffer = new char[4096];
//            StringBuilder output = new StringBuilder();
//            while ((i = reader.read(buffer)) > 0)
//                output.append(buffer, 0, i);
//            reader.close();
//            str = output.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        int pos=str.indexOf("Average");
//        pos+=11;
//        char[] characters=str.toCharArray();
//        ping=characters[pos]+characters[pos+1]+"ms";
//    }

    private synchronized void switchOnClick(){
        if (notificationTracker){
            notificationTracker=false;
            intentService=new Intent(this,NotificationService.class);
            stopService(intentService);
        }else{
            startForegroundService(intentService);
            notificationTracker=true;
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private List<ApplicationInfo> getAppInfo(){
        final PackageManager pm=getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    @SuppressLint("SimpleDateFormat")
    private void recyclerViewLoading(NetworkType type,DATERV daterv){
        binding.recyclerView.setVisibility(View.GONE);
        long start;
        if (daterv==DATERV.MONTH)
             start=currentMonthMillis();
        else
            start=currentDateMillis();
        monthlyUsage=new ArrayList<>();
        dailyUsage=new ArrayList<>();
        List<String> names=new ArrayList<>();
        List<Drawable> images=new ArrayList<>();
        List<RowObject> rowList=new ArrayList<>();
        double totalWifi;
        if (type==NetworkType.WIFI)
             totalWifi=retrieveDataUsage(rowList,start,ConnectivityManager.TYPE_WIFI);
        else
             totalWifi=retrieveDataUsage(rowList,start,ConnectivityManager.TYPE_MOBILE);

        Collections.sort(rowList);
        setUpRow(rowList);

        for (RowObject r:rowList) {
            monthlyUsage.add(r.getUsage());
            dailyUsage.add(r.getUsage());
            names.add(r.getName());
            images.add(r.getSlika());
        }

        MyAdapter adapter=new MyAdapter(this,monthlyUsage,dailyUsage,names,images);
        binding.recyclerView.setAdapter(adapter);
        totalWifi/=1000;
        DecimalFormat df=new DecimalFormat("0.00");
        totalWifi=Double.parseDouble(df.format(totalWifi));
        String totalWifiUsage="Total: "+ totalWifi+"GB";
        binding.wifiTotal.setText(totalWifiUsage);
        binding.recyclerView.setVisibility(View.VISIBLE);
    }

    private void setUpRow(List<RowObject> rowList) {
        for (RowObject r:rowList){
            if(r.getUsageTemp()>1000){
                r.setUsageTemp(r.getUsageTemp()/1000);
                r.formatUsage();
                r.setUsage(r.getUsageTemp()+"GB");
            }else
            {
                r.formatUsage();
                r.setUsage(r.getUsageTemp()+"MB");
            }
        }
    }

    public long currentDateMillis(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    @SuppressLint("SimpleDateFormat")
    public long currentMonthMillis(){
        Calendar monthlyCal = Calendar.getInstance();
         DateFormat dateFormat = new SimpleDateFormat("MM");
        Date date = new Date();
        int month=Integer.parseInt(dateFormat.format(date));
        dateFormat=new SimpleDateFormat("yyyy");
        int yearCur=Integer.parseInt(dateFormat.format(date));

        monthlyCal.set(Calendar.YEAR,yearCur);
        monthlyCal.set(Calendar.MONTH,--month);
        monthlyCal.set(Calendar.DAY_OF_MONTH,1);
        monthlyCal.set(Calendar.HOUR_OF_DAY,1);
        monthlyCal.set(Calendar.MINUTE,1);
        monthlyCal.set(Calendar.SECOND,1);
        return monthlyCal.getTimeInMillis();
    }

    public double retrieveDataUsage(List<RowObject> rowList,long startTime,int type){
        List<ApplicationInfo> appInfo=getAppInfo();
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.DATE,3);
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
        double temp=0;
        double total=0;
        NetworkStats networkStats1;
        NetworkStats.Bucket bucket=new NetworkStats.Bucket();
        PackageManager pm=getPackageManager();

        for(ApplicationInfo info:appInfo) {
            if (type==ConnectivityManager.TYPE_WIFI)
                 networkStats1 = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, startTime, cal.getTimeInMillis(), info.uid);
            else
                networkStats1 = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null, startTime, cal.getTimeInMillis(), info.uid);
            while(networkStats1.hasNextBucket()) {
                networkStats1.getNextBucket(bucket);
                temp += ((double) bucket.getRxBytes()) / MILLION;
                temp += ((double) bucket.getTxBytes()) / MILLION;
            }
            if(temp>MIN_DATA_AMOUNT_IN_MB) {
                total+=temp;
                RowObject row=new RowObject();
                String name=pm.getApplicationLabel(info).toString();
                row.setUsageTemp(temp);
                row.setName(name);
                row.setSlika(pm.getApplicationIcon(info));
                rowList.add(row);
            }
            temp=0;
        }
        return total;
    }

    public void recyclerViewWorkingThread(NetworkType type) {
        Runnable runnable = () -> {
            if (getGrantStatus())
                binding.radioWifi.performClick();
            loaded=true;
        };

        Thread thread = new Thread(runnable);
        thread.start();

    }

    public void settingsWorkingThread(){
        Runnable runnable= this::loadSettings;
        Thread thread=new Thread(runnable);
        thread.start();
    }

    public void loadSettings(){
        List<String> titles=new ArrayList<>();
        titles.add("Feedback");
        titles.add("Privacy Policy");
        titles.add("FAQ");
        titles.add("Rate the App");

        List<String> descriptions=new ArrayList<>();
        descriptions.add("Change the app");
        descriptions.add("Read about what we do with your information");
        descriptions.add("Questions about the app");
        descriptions.add("Show your love on the playstore");
        SettingsAdapter settingsAdapter=new SettingsAdapter(this,titles,descriptions);
        binding.recyclerViewSettings.setAdapter(settingsAdapter);
        binding.recyclerViewSettings.addItemDecoration(new DividerItemDecoration(binding.recyclerViewSettings.getContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onDestroy() {
        stopService(intentService);
        super.onDestroy();
    }
}