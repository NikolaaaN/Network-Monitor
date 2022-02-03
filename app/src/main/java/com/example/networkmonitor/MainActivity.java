package com.example.networkmonitor;
//nazovi je my internet tracker lite

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.networkmonitor.databinding.ActivityMainBinding;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;


public class MainActivity extends AppCompatActivity {

    private static final long MILLION=1048576;
    private static final long SECOND=1000;

    private ActivityMainBinding binding;
    private boolean notificationTracker;
    private String download;
    private String upload;
    private String ping;
    private boolean downloading=true;
    private Semaphore semafor;
    private List<String> monthlyUsage;
    private List<String> dailyUsage;
    private static final int PERMISSION_CODE=1;
    private Intent intentService;
    private boolean loaded=false;
    String[] permissions={Manifest.permission.INTERNET};

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
        download="";
        upload="";
        ping="";
        semafor=new Semaphore(0);
        monthlyUsage=new ArrayList<>();
        dailyUsage=new ArrayList<>();

        intentService=new Intent(this,NotificationService.class);
        startForegroundService(intentService);

        requestPermissions();
        setListeners();
        updateUI();
        recyclerViewWorkingThread();



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

   //sa stacka
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
                    long tempDown = TrafficStats.getTotalRxBytes();
                    long tempUp=TrafficStats.getTotalTxBytes();
                    long total = TrafficStats.getTotalRxBytes() - usageAtStart;


                    try {
                        Thread.sleep(SECOND);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    double currDown=(double)TrafficStats.getTotalRxBytes()-tempDown;
                    double currUp=(double)TrafficStats.getTotalTxBytes()-tempUp;

                    handler.post(()-> {
                        DecimalFormat df = new DecimalFormat("0.00");

                        String totalString =df.format(total / MILLION) + "MB";
                        binding.total.setText(totalString);
                        String currDownString = df.format(currDown / MILLION) + "MB/s";
                        binding.currentDown.setText(currDownString);
                        String currUpString= df.format(currUp/MILLION) + "MB/s";
                        binding.currentUp.setText(currUpString);

                    });
                }
            }
        };

        Thread start=new Thread(monitor);
        start.start();

    }

    @SuppressLint({"NonConstantResourceId", "ClickableViewAccessibility"})
    private void setListeners(){
        binding.navbar.setOnItemSelectedListener(item-> {
                switch (item.getItemId()){
                    case R.id.monitor:
                        binding.card1.setVisibility(View.VISIBLE);
                        binding.card2.setVisibility(View.GONE);
                        binding.card3.setVisibility(View.GONE);
                        return true;

                    case R.id.usage:
                        if(loaded) {
                            binding.card1.setVisibility(View.GONE);
                            binding.card2.setVisibility(View.GONE);
                            binding.card3.setVisibility(View.VISIBLE);
                            return true;
                        }
                    case R.id.settings:
                        binding.card1.setVisibility(View.GONE);
                        binding.card2.setVisibility(View.VISIBLE);
                        binding.card3.setVisibility(View.GONE);
                        return true;
                }
                return false;
        });

        binding.switchTracker.setOnClickListener(v-> switchOnClick());

        binding.constraintLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                if (binding.card1.getVisibility()==View.VISIBLE){
                    binding.navbar.setSelectedItemId(R.id.settings);
                    return;
                }

                if (binding.card2.getVisibility()==View.VISIBLE){
                    binding.navbar.setSelectedItemId(R.id.usage);
                }
            }
            @Override
            public void onSwipeRight(){
                if (binding.card2.getVisibility()==View.VISIBLE){
                    binding.navbar.setSelectedItemId(R.id.monitor);
                    return;
                }
                if (binding.card3.getVisibility()==View.VISIBLE){
                    binding.navbar.setSelectedItemId(R.id.settings);
                }
            }
        });
    }


    //ako zatreba neka ostane
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
    private void recyclerViewLoading(){
        Calendar monthlyCal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("MM");
        Date date = new Date();
        int month=Integer.parseInt(dateFormat.format(date));
        dateFormat=new SimpleDateFormat("yyyy");
        int yearCur=Integer.parseInt(dateFormat.format(date));
        dateFormat=new SimpleDateFormat("DD");
        int dayCur=Integer.parseInt(dateFormat.format(date));

        boolean monthly=false;
        boolean daily=true;

        monthlyCal.set(Calendar.YEAR,yearCur);
        monthlyCal.set(Calendar.MONTH,--month);
        monthlyCal.set(Calendar.DAY_OF_MONTH,1);
        monthlyCal.set(Calendar.HOUR_OF_DAY,1);
        monthlyCal.set(Calendar.MINUTE,1);
        monthlyCal.set(Calendar.SECOND,1);

        long startTimeMonthly = monthlyCal.getTimeInMillis();

        LocalDate localDate=LocalDate.now();

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long millis = c.getTimeInMillis();

        Calendar dailyCal=Calendar.getInstance();
        dailyCal.set(Calendar.YEAR,localDate.getYear());
        dailyCal.set(Calendar.MONTH,localDate.getMonth().getValue()-1);
        dailyCal.set(Calendar.DAY_OF_MONTH,localDate.getDayOfMonth());
        dailyCal.set(Calendar.MINUTE,0);
        dailyCal.set(Calendar.SECOND,0);
        dailyCal.set(Calendar.HOUR,0);

        long startTimeDaily = dailyCal.getTimeInMillis();
        Log.wtf("vreme",millis+"");
        Log.wtf("vreme",startTimeMonthly+"");

        PackageManager pm=getPackageManager();
        List<String> names=new ArrayList<>();
        List<Drawable> images=new ArrayList<>();
        List<ApplicationInfo> appInfo=getAppInfo();

      Calendar cal=Calendar.getInstance();
      cal.add(Calendar.DATE,3);

       List<RowObject> rowList=new ArrayList<>();

        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
            double tempM=0;
            double tempD=0;
            double totalWifi=0;
            NetworkStats networkStats1;
            NetworkStats networkStats2;
            NetworkStats.Bucket bucketMonthly=new NetworkStats.Bucket();
            NetworkStats.Bucket bucketDaily=new NetworkStats.Bucket();
                for(ApplicationInfo info:appInfo) {
                    networkStats1 = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, startTimeMonthly, cal.getTimeInMillis(),info.uid);
                    networkStats2 = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, millis,cal.getTimeInMillis(),info.uid);
                    Log.d("networkStats",networkStats2.toString());
                    while(networkStats1.hasNextBucket() && monthly) {
                        networkStats1.getNextBucket(bucketMonthly);
                        tempM += ((double) bucketMonthly.getRxBytes()) / MILLION;
                        tempM += ((double) bucketMonthly.getTxBytes()) / MILLION;

                    }//ovde je vrv problem podeli ovaj while na dva gde je drugi za networkStats2.gasNextBucket()


                    while(networkStats2.hasNextBucket() && daily){
                        networkStats2.getNextBucket(bucketDaily);
                        Log.d("bucket",bucketDaily.getRxBytes()+"");
                        tempD += ((double)bucketDaily.getRxBytes())/MILLION;
                        tempD += ((double)bucketDaily.getTxBytes())/MILLION;
                    }

                    if(tempM>1 && monthly) {
                        totalWifi+=tempM;
                        RowObject row=new RowObject();

                        String name=pm.getApplicationLabel(info).toString();
                        row.setUsageTemp(tempM);
                        row.setName(name);
                        row.setSlika(pm.getApplicationIcon(info));
                        rowList.add(row);
                    }
                    if (tempD>1 && daily){
                        totalWifi+=tempD;
                        RowObject row=new RowObject();
                        row.setUsageTemp(tempD);
                        rowList.add(row);
                        String name=pm.getApplicationLabel(info).toString();
                        names.add(name);
                        images.add(pm.getApplicationIcon(info));
                    }
                    tempM=0;
                    tempD=0;
                }
                Collections.sort(rowList);
                setUpRow(rowList);
                for (RowObject r:rowList) {
                    monthlyUsage.add(r.getUsage());
                    dailyUsage.add(r.getUsage());
                    names.add(r.getName());
                    images.add(r.getSlika());
                }

                MyAdapter adapter=new MyAdapter(this,dailyUsage,dailyUsage,names,images);
                binding.recyclerView.setAdapter(adapter);
                totalWifi/=1000;
                DecimalFormat df=new DecimalFormat("0.00");
                totalWifi=Double.parseDouble(df.format(totalWifi));
                String totalWifiUsage="Wifi: "+ totalWifi+"GB";
                binding.wifiTotal.setText(totalWifiUsage);
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

    public void recyclerViewWorkingThread() {
        Runnable runnable = () -> {
            if (getGrantStatus())
                recyclerViewLoading();
            loaded=true;
        };

        Thread thread = new Thread(runnable);
        thread.start();

    }

    @Override
    protected void onDestroy() {
        stopService(intentService);
        super.onDestroy();
    }
}