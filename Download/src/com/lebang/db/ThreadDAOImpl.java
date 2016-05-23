package com.lebang.db;

import java.util.ArrayList;
import java.util.List;

import com.lebang.entity.ThreadInfo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 数据访问接口的实现类 DAOImpl        DAO确定要做的事，DAOImpl具体去执行。 同时，DAOImpl中找了一个帮手DBHelper,简化SQLite的语法编写
 * @author Administrator
 */
public class ThreadDAOImpl implements ThreadDAO {

	private DBHelper mHelper = null;

	public ThreadDAOImpl(Context context) {
		mHelper = new DBHelper(context);
	}

	@Override
	public void insertThread(ThreadInfo threadInfo) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.execSQL(
				"insert into thread_info(id,url,start,end,finished) value ("
		+ threadInfo.getId()+","+ threadInfo.getUrl() +"," +threadInfo.getStart()+ ","
		+ threadInfo.getEnd()+ ","+threadInfo.getFinished()+")");
//		db.execSQL(
//				"insert into thread_info(thread_id,url,start,end,finished) value (?,?,?,?,?)",
//				new Object[] { threadInfo.getId(), threadInfo.getUrl(), threadInfo.getStart(), threadInfo.getEnd(),
//						threadInfo.getFinished() });
		db.close();
	}

	@Override
	public void deleteThread(String url, int thread_id) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.execSQL("delete from thread_info where url = ? and thread_id =?", new Object[] { url, thread_id });
		db.close();
	}

	@Override
	public void updateThread(String url, int thread_id, int finished) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		db.execSQL("update thread_info set finished = ? where url = ? and thread_id =?",
				new Object[] { finished, url, thread_id });
		db.close();
	}

	@Override
	public List<ThreadInfo> getThreads(String url) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		List<ThreadInfo> list = new ArrayList<ThreadInfo>();
		Cursor cursor = db.rawQuery("select * from thread_info where url = ?", new String[] { url });
		while (cursor.moveToNext()) {
			ThreadInfo threadinfo = new ThreadInfo();
			threadinfo.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
			threadinfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
			threadinfo.setStart(cursor.getInt(cursor.getColumnIndex("start")));
			threadinfo.setEnd(cursor.getInt(cursor.getColumnIndex("end")));
			threadinfo.setFinished(cursor.getInt(cursor.getColumnIndex("finished")));
			list.add(threadinfo);
		}
		cursor.close();
		db.close();
		return list;
	}

	@Override
	public boolean isExists(String url, int thread_id) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		Cursor cursor = db.rawQuery("select * from thread_info where url = ? and thread_id = ?",
				new String[] { url, thread_id + "" });
		boolean exists = cursor.moveToNext();
		cursor.close();
		db.close();
		return exists;
	}
}