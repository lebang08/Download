package com.lebang.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.lebang.db.ThreadDAO;
import com.lebang.db.ThreadDAOImpl;
import com.lebang.entity.FileInfo;
import com.lebang.entity.ThreadInfo;

import android.content.Context;
import android.content.Intent;

/**
 * ����������
 * ��ɾ������������������ִ�й�����ͨ���㲥�� ��intent���ظ�Activity������UI��������Progressbar��
 * �ϵ�����ͨ���������ݿ�DAO��ʵ��
 * @author Administrator
 */
public class DownloadTask {
	private Context mContext = null;
	private FileInfo mFileInfo = null;
	private ThreadDAO mDao = null;
	private int mFinished = 0;
	public boolean isPause = false;

	public DownloadTask(Context mContext, FileInfo mFileInfo) {
		this.mContext = mContext;
		this.mFileInfo = mFileInfo;
		mDao = new ThreadDAOImpl(mContext);
	}

	public void download() {
		// ��ȡ���ݿ���߳���Ϣ
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
		ThreadInfo threadInfo = null;
		if (threadInfos.size() == 0) {
			// ��ʼ���߳���Ϣ����
			threadInfo = new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		} else {
			threadInfo = threadInfos.get(0);
		}
		// �������߳̽�������
		new DownloadThread(threadInfo).start();
	}

	class DownloadThread extends Thread {
		private ThreadInfo mThreadInfo = null;

		public DownloadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo;
		}

		public void run() {
			// �����ݿ�����߳���Ϣ
			if (!mDao.isExists(mThreadInfo.getUrl(), mThreadInfo.getId())) {
				mDao.insertThread(mThreadInfo);
			}
			HttpURLConnection conn = null;
			RandomAccessFile raf = null;
			InputStream input = null;
			try {
				URL url = new URL(mThreadInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				// ��������λ��
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes = " + start + "-" + mThreadInfo.getEnd());
				// �����ļ�д��λ��
				File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent it = new Intent(DownloadService.ACTION_UPDATE);
				mFinished += mThreadInfo.getFinished();
				// ��ʼ����
				if (conn.getResponseCode() == 206) {
					// ��ȡ����
					input = conn.getInputStream();
					byte[] buffer = new byte[1024 * 4];
					int len = -1;
					long time = System.currentTimeMillis();
					while ((len = input.read(buffer)) != -1) {
						// д���ļ�
						raf.write(buffer, 0, len);
						// �����ؽ��ȷ��͹㲥��Activity
						mFinished += len;
						if (System.currentTimeMillis() - time > 500) {
							// �԰ٷֱȵ���ʽ��ֵ
							it.putExtra("finished", mFinished * 100 / mFinished);
							mContext.sendBroadcast(it);
						}
						// ������ͣʱ�������
						if (isPause) {
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mFinished);
							return;
						}
					}
					// ɾ���߳���Ϣ
					mDao.deleteThread(mThreadInfo.getUrl(), mThreadInfo.getId());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					conn.disconnect();
					input.close();
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}