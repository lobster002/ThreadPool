package com.sky;

import java.util.LinkedList;
import java.util.List;

public final class ThreadPool {
	
	
	private static int COUNT = 1;// 线程池中默认线程的个数为1
	
	private WorkThread[] workThrads;// 工作线程
	
	private static volatile int FINISH_COUNT = 0; // 未处理的任务  volatile关键字 保证代码不会被重排序
	
	private List<Runnable> taskQueue = new LinkedList<Runnable>();// 任务队列，作为一个缓冲,List线程不安全
	private static ThreadPool threadPool;

	
	private ThreadPool() {// 创建具有默认线程个数的线程池
		this(5);
	}

	
	private ThreadPool(int worker_num) { // 创建线程池,worker_num为线程池中工作线程的个数
		ThreadPool.COUNT = worker_num;
		workThrads = new WorkThread[COUNT];
		for (int i = 0; i < worker_num; i++) {
			workThrads[i] = new WorkThread();
			workThrads[i].start();// 开启线程池中的线程
		}
	}

	
	public static ThreadPool getThreadPool() {// 单态模式，获得一个默认线程个数的线程池
		return getThreadPool(ThreadPool.COUNT);
	}

	/*
	 *  单态模式，获得一个指定线程个数的线程池,worker_num(>0)为线程池中工作线程的个数
	 * worker_num<=0创建默认的工作线程个数
	 */
	public static ThreadPool getThreadPool(int worker_num1) {
		if (worker_num1 <= 0)
			worker_num1 = ThreadPool.COUNT;
		if (threadPool == null)
			threadPool = new ThreadPool(worker_num1);
		return threadPool;
	}

	// 执行任务,其实只是把任务加入任务队列，什么时候执行有线程池管理器决定
	public void addtask(Runnable task) {
		synchronized (taskQueue) {//锁定
			taskQueue.add(task);
			taskQueue.notify();//释放
		}
	}

	// 批量执行任务,其实只是把任务加入任务队列，什么时候执行有线程池管理器决定
	public void addtask(Runnable[] task) {
		synchronized (taskQueue) {
			for (Runnable t : task)
				taskQueue.add(t);
			taskQueue.notify();//唤醒其他线程
		}
	}

	// 批量执行任务,其实只是把任务加入任务队列，什么时候执行由线程池管理器决定
	public void addtask(List<Runnable> task) {
		synchronized (taskQueue) {
			for (Runnable t : task)
				taskQueue.add(t);
			taskQueue.notify();
		}
	}

	// 销毁线程池,该方法保证在所有任务都完成的情况下才销毁所有线程，否则等待任务完成才销毁
	public void destroy() {
		while (!taskQueue.isEmpty()) {// 如果还有任务没执行完成，就先睡会吧
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// 工作线程停止工作，且置为null
		for (int i = 0; i < COUNT; i++) {
			workThrads[i].stopWorker();
			workThrads[i] = null;
		}
		threadPool = null;
		taskQueue.clear();// 清空任务队列
	}

	// 返回工作线程的个数
	public int getWorkThreadNumber() {
		return COUNT;
	}

	// 返回已完成任务的个数,这里的已完成是只出了任务队列的任务个数，可能该任务并没有实际执行完成
	public int getFinishedTasknumber() {
		return FINISH_COUNT;
	}

	// 返回任务队列的长度，即还没处理的任务个数
	public int getWaitTasknumber() {
		return taskQueue.size();
	}

	// 覆盖toString方法，返回线程池信息：工作线程个数和已完成任务个数
	@Override
	public String toString() {
		return "WorkThread number:" + COUNT + " and  finished task number:"
				+ FINISH_COUNT + "  wait task number:" + getWaitTasknumber();
	}

	private class WorkThread extends Thread {//内部类 工作线程
		
		private boolean isRunning = true;// 该工作线程是否有效，用于结束该工作线程

		@Override
		public void run() {
			Runnable r = null;
			while (isRunning) {// 注意，若线程无效则自然结束run方法，该线程就没用了
				synchronized (taskQueue) {
					while (isRunning && taskQueue.isEmpty()) {
						try {
							taskQueue.wait(20);//关键所在啊，如果任务队列不空，则取出任务执行，若任务队列空，则等待
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (!taskQueue.isEmpty())
						r = taskQueue.remove(0);// 取出任务
				}
				if (r != null) {
					r.run();// 执行任务
					FINISH_COUNT++;//任务数+1
				}
				r = null;
			}
		}

		public void stopWorker() {
			// 停止工作，让该线程自然执行完当前任务后，自然结束
			isRunning = false;
		}
	}
}