package com.example.servicedownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

import javax.net.ssl.SSLSessionBindingEvent;

public class DownloadService extends Service {

    private DownloadTask downloadTask;

    private String downloadUrl;

    public static final int SysProgress = -1;

    public DownloadService() {
    }

    //首先创建一个DownloadListener匿名类实例对象  在匿名类中实现onProgress onSuccess onFailed onPaused onCancel
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            //getNotification方法构建一个用于显示下载进度的通知 然后调用getNotificationManager的notify方法去触发这个通知
            //可实时查看当前下载的进度
            getNotificationManager().notify(1,getNotification("正在下载...",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功时将前台服务通知关闭,并创建下一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载成功",SysProgress));
            Toast.makeText(DownloadService.this,"下载成功",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            //下载失败时将前台服务通知关闭,并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("下载失败",SysProgress));
            Toast.makeText(DownloadService.this,"下载失败",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this,"下载暂停",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"下载取消",Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder mBinder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }
    //为了要让DownloadService可以和活动进行通信
    class DownloadBinder extends Binder{

        public void startDownload(String url){
            if(downloadTask == null){
                downloadUrl = url;
                //新建一个DownloadTask实例 将DownloadListener作为参数传入
                downloadTask = new DownloadTask(listener);
                //然后调用execute方法开启下载  将downloadTask作为URL地址传入到execute方法中
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("正在下载...",0));
                Toast.makeText(DownloadService.this,"正在下载...",Toast.LENGTH_SHORT).show();
            }
        }
        public void pauseDownload(){
            if(downloadTask != null){
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if(downloadTask != null){
                downloadTask.cancelDownload();
                //取消下载时需将文件删除，并通知关闭
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.
                        getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if(file.exists()){
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this,"下载已取消",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if(progress>0){
            //当progress大于或者等于0时才需显示下载进度
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }
}
