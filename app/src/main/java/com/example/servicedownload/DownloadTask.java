package com.example.servicedownload;

import android.os.AsyncTask;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by 付存哲kk on 2017/3/13.
 */
//第一个泛型String表示在执行AsyncTask的时候需要传入一个字符串参数给后台
    //第二个泛型Integer表示使用整型数据来作为进度显示单位
        //第三个泛型Integer表示使用整型数据来反馈执行结果
public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress;

    public DownloadTask(DownloadListener listener){
        this.listener = listener;
    }
    //用于在后台执行具体的下载逻辑
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadLength = 0;//记录已下载的文件长度
            String downloadUrl = params[0];//从参数中获取到了下载的URL地址
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));//解析出了下载的文件名
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if(file.exists()){
                downloadLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if(contentLength == 0){
                return TYPE_FAILED;
            }else if(contentLength == downloadLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadLength+"-")//断点下载，指定从哪个字节开始下载
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if(response!=null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadLength);//跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len = is.read(b))!=-1){
                    //判断用户有没有触发暂停或者取消的操作   如果有就返回TYPE_CANCELED或者TYPE_PAUSED来中断当前的下载
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else {//如果没有上述操作  就实时计算当前的下载进度 然后调用publishProgress方法进行通知
                        total += len;
                        savedFile.write(b,0,len);
                        //计算已下载的百分比
                        int progress = (int) ((total+downloadLength) * 100/contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
                }
            } catch (Exception e) {
                e.printStackTrace();
        }finally {
            try {
                if (is != null) {
                    is.close();
                }
                if(savedFile != null){
                    savedFile.close();
                }
                if(isCanceled && file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }
    //用于在界面上更新当前的下载进度
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }
    //用于通知最终的下载结果
    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }
    //首先获取待下载文件的总长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if(response!=null&&response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}
