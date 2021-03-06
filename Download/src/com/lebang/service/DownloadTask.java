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
 * 下载任务类
 * 完成具体的下载任务，在任务执行过程中通过广播， 将intent传回给Activity，更新UI（进度条Progressbar）
 * 断点续传通过访问数据库DAO来实现
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
		// 读取数据库的线程信息
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
		ThreadInfo threadInfo = null;
		if (threadInfos.size() == 0) {
			// 初始化线程信息对象
			threadInfo = new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		} else {
			threadInfo = threadInfos.get(0);
		}
		// 创建子线程进行下载
		new DownloadThread(threadInfo).start();
	}

	class DownloadThread extends Thread {
		private ThreadInfo mThreadInfo = null;

		public DownloadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo;
		}

		public void run() {
			// 向数据库插入线程信息
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
				// 设置下载位置
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes = " + start + "-" + mThreadInfo.getEnd());
				// 设置文件写入位置
				File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent it = new Intent(DownloadService.ACTION_UPDATE);
				mFinished += mThreadInfo.getFinished();
				// 开始下载
				if (conn.getResponseCode() == 206) {
					// 读取数据
					input = conn.getInputStream();
					byte[] buffer = new byte[1024 * 4];
					int len = -1;
					long time = System.currentTimeMillis();
					while ((len = input.read(buffer)) != -1) {
						// 写入文件
						raf.write(buffer, 0, len);
						// 把下载进度发送广播给Activity
						mFinished += len;
						if (System.currentTimeMillis() - time > 500) {
							// 以百分比的形式传值
							it.putExtra("finished", mFinished * 100 / mFinished);
							mContext.sendBroadcast(it);
						}
						// 下载暂停时保存进度
						if (isPause) {
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mFinished);
							return;
						}
					}
					// 删除线程信息
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