package com.example.servicedownload;

/**
 * Created by 付存哲kk on 2017/3/13.
 */

public interface DownloadListener {

    //通知当前下载进度
    void onProgress(int progress);
    //通知下载成功事件
    void onSuccess();
    //用于通知下载失败事件
    void onFailed();
    //用于通知下载暂停事件
    void onPaused();
    //用于通知下载取消事件
    void onCanceled();
}
