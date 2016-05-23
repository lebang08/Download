package com.lebang.db;

import java.util.List;

import com.lebang.entity.ThreadInfo;

/**
 * 数据访问接口
 * @author Administrator
 */

public interface ThreadDAO {
	
	public void insertThread(ThreadInfo threadInfo);
	public void deleteThread(String url,int thread_id);
	public void updateThread(String url,int thread_id,int finished);
	public List<ThreadInfo> getThreads(String url);
	
	public boolean isExists(String url,int thread_id);
}
