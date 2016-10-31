package com.lebang.download;

import com.lebang.entity.FileInfo;
import com.lebang.service.DownloadService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	private TextView txt;
	private ProgressBar progress;
	private Button btnStart, btnStop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		initView();
		// 注册广播接收器
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_UPDATE);
		registerReceiver(mRecevier, filter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mRecevier);
	}
	
	BroadcastReceiver mRecevier = new BroadcastReceiver() {
		public void onReceive(android.content.Context context, Intent intent) {
			if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
				int finished = intent.getIntExtra("finished", 0);
				progress.setProgress(finished);
			}
		};
	};

	private void initView() {
		txt = (TextView) findViewById(R.id.text_filename);
		progress = (ProgressBar) findViewById(R.id.progress);
		btnStart = (Button) findViewById(R.id.btn_download);
		btnStop = (Button) findViewById(R.id.btn_cancel);
		progress.setMax(100);

		/* 创建文件信息对象 */
		final FileInfo fileInfo = new FileInfo(0, "http://www.iyuce.com/uploadfiles/app/woyuce.apk", "Myfilename", 0,0);
		btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/* 通过Intent 传递参数给Service */
				Intent intent = new Intent(MainActivity.this, DownloadService.class);
				intent.setAction(DownloadService.ACTION_START);
				intent.putExtra("fileInfo", fileInfo);
				startService(intent);
			}
		});

		btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/* 通过Intent 传递参数给Service */
				Intent intent = new Intent(MainActivity.this, DownloadService.class);
				intent.setAction(DownloadService.ACTION_STOP);
				intent.putExtra("fileInfo", fileInfo);
				startService(intent);
			}
		});
	}
}