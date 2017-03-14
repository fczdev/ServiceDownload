package com.example.servicedownload;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.xml.sax.helpers.DefaultHandler;

public class MainActivity extends AppCompatActivity {

    private DownloadService.DownloadBinder downloadBinder;

    public Button startDownload;

    public Button pauseDownload;

    public Button cancelDownload;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获取到downloadBinder的实例
            downloadBinder = (DownloadService.DownloadBinder) service;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startDownload = (Button) findViewById(R.id.start_download);
        pauseDownload = (Button) findViewById(R.id.pause_download);
        cancelDownload = (Button) findViewById(R.id.cancel_download);
        Intent intent = new Intent(this,DownloadService.class);
        startService(intent);//启动服务
        bindService(intent,connection,BIND_AUTO_CREATE);//绑定服务  让MainActivity和DownloadService进行通信
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(downloadBinder == null){
                    return;
                }
                String url = "http://sw.bos.baidu.com/sw-search-sp/software/952c9d6e73f50/QQ_8.9.20029.0_setup.exe";
                downloadBinder.startDownload(url);
            }
        });

        pauseDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(downloadBinder == null){
                    return;
                }
                downloadBinder.pauseDownload();
            }
        });

        cancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(downloadBinder == null){
                    return;
                }
                downloadBinder.cancelDownload();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this,"拒绝权限将无法使用程序",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
