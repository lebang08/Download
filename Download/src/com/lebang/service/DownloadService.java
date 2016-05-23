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
 * ����Activity�д��ݹ�����Action�źţ�����Ӧ�ĳ�ʼ��������ȡ�������ļ��Ĵ�С�ȣ�  ---��������������񣬽���DownloadTask
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
		/* ���activity�����Ĳ��� */
		if (ACTION_START.equals(intent.getAction())) {
			FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
			Log.e("FileInfo", "Start:" + fileInfo.toString());
			// ������ʼ���߳�
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
				// ������ʼ������ -------------------------------->����DownloadTaskȥִ�о������������
				mTask = new DownloadTask(DownloadService.this,fileInfo);
				mTask.download();
				break;
			}
		};
	};

	/**
	 * ��ʼ�����߳�
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
				// ���������ļ�
				URL url = new URL(fileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				// ����ļ�����
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
				// �ڱ��ش����ļ�
				File file = new File(dir, fileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				// �����ļ�����
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