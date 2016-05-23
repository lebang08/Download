package com.lebang.service;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import com.lebang.entity.FileInfo;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * 接收Activity中传递过来的Action信号，做相应的初始化处理（获取待下载文件的大小等）  ---而具体的下载任务，交给DownloadTask
 * @author Administrator
 */
public class DownloadService extends Service {

	public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/downloads";
	public static final String ACTION_START = "ACTION_START";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final int MSG_INIT = 0;
	private DownloadTask mTask = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/* 获得activity传来的参数 */
		if (ACTION_START.equals(intent.getAction())) {
			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
			Log.e("FileInfo", "Start:" + fileInfo.toString());
			// 启动初始化线程
			new InitThread(fileInfo).start();
		} else if (ACTION_STOP.equals(intent.getAction())) {
			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
			Log.e("FileInfo", "Stop:" + fileInfo.toString());
			if(mTask != null){
				mTask.isPause = true;
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_INIT:
				FileInfo fileInfo = (FileInfo) msg.obj;
				Log.e("test", "init :" + fileInfo);
				// 启动初始化任务 -------------------------------->启动DownloadTask去执行具体的下载任务
				mTask = new DownloadTask(DownloadService.this,fileInfo);
				mTask.download();
				break;
			}
		};
	};

	/**
	 * 初始化子线程
	 */
	class InitThread extends Thread {
		private FileInfo fileInfo;

		public InitThread(FileInfo fileInfo) {
			super();
			this.fileInfo = fileInfo;
		}

		public void run() {
			HttpURLConnection conn = null;
			RandomAccessFile raf = null;
			try {
				// 链接网络文件
				URL url = new URL(fileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				// 获得文件长度
				int length = -1;
				if (conn.getResponseCode() == 200) {
					length = conn.getContentLength();
				}
				if (length <= 0) {
					return;
				}
				File dir = new File(DOWNLOAD_PATH);
				if (!dir.exists()) {
					dir.mkdir();
				}
				// 在本地创建文件
				File file = new File(dir, fileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				// 设置文件长度
				raf.setLength(length);
				fileInfo.setLength(length);
				handler.obtainMessage(MSG_INIT, fileInfo).sendToTarget();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					conn.disconnect();
					raf.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}