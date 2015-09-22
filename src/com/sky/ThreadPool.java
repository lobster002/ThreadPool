package com.sky;

import java.util.LinkedList;
import java.util.List;

public final class ThreadPool {
	
	
	private static int COUNT = 1;// �̳߳���Ĭ���̵߳ĸ���Ϊ1
	
	private WorkThread[] workThrads;// �����߳�
	
	private static volatile int FINISH_COUNT = 0; // δ���������  volatile�ؼ��� ��֤���벻�ᱻ������
	
	private List<Runnable> taskQueue = new LinkedList<Runnable>();// ������У���Ϊһ������,List�̲߳���ȫ
	private static ThreadPool threadPool;

	
	private ThreadPool() {// ��������Ĭ���̸߳������̳߳�
		this(5);
	}

	
	private ThreadPool(int worker_num) { // �����̳߳�,worker_numΪ�̳߳��й����̵߳ĸ���
		ThreadPool.COUNT = worker_num;
		workThrads = new WorkThread[COUNT];
		for (int i = 0; i < worker_num; i++) {
			workThrads[i] = new WorkThread();
			workThrads[i].start();// �����̳߳��е��߳�
		}
	}

	
	public static ThreadPool getThreadPool() {// ��̬ģʽ�����һ��Ĭ���̸߳������̳߳�
		return getThreadPool(ThreadPool.COUNT);
	}

	/*
	 *  ��̬ģʽ�����һ��ָ���̸߳������̳߳�,worker_num(>0)Ϊ�̳߳��й����̵߳ĸ���
	 * worker_num<=0����Ĭ�ϵĹ����̸߳���
	 */
	public static ThreadPool getThreadPool(int worker_num1) {
		if (worker_num1 <= 0)
			worker_num1 = ThreadPool.COUNT;
		if (threadPool == null)
			threadPool = new ThreadPool(worker_num1);
		return threadPool;
	}

	// ִ������,��ʵֻ�ǰ��������������У�ʲôʱ��ִ�����̳߳ع���������
	public void addtask(Runnable task) {
		synchronized (taskQueue) {//����
			taskQueue.add(task);
			taskQueue.notify();//�ͷ�
		}
	}

	// ����ִ������,��ʵֻ�ǰ��������������У�ʲôʱ��ִ�����̳߳ع���������
	public void addtask(Runnable[] task) {
		synchronized (taskQueue) {
			for (Runnable t : task)
				taskQueue.add(t);
			taskQueue.notify();//���������߳�
		}
	}

	// ����ִ������,��ʵֻ�ǰ��������������У�ʲôʱ��ִ�����̳߳ع���������
	public void addtask(List<Runnable> task) {
		synchronized (taskQueue) {
			for (Runnable t : task)
				taskQueue.add(t);
			taskQueue.notify();
		}
	}

	// �����̳߳�,�÷�����֤������������ɵ�����²����������̣߳�����ȴ�������ɲ�����
	public void destroy() {
		while (!taskQueue.isEmpty()) {// �����������ûִ����ɣ�����˯���
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// �����߳�ֹͣ����������Ϊnull
		for (int i = 0; i < COUNT; i++) {
			workThrads[i].stopWorker();
			workThrads[i] = null;
		}
		threadPool = null;
		taskQueue.clear();// ����������
	}

	// ���ع����̵߳ĸ���
	public int getWorkThreadNumber() {
		return COUNT;
	}

	// �������������ĸ���,������������ֻ����������е�������������ܸ�����û��ʵ��ִ�����
	public int getFinishedTasknumber() {
		return FINISH_COUNT;
	}

	// ����������еĳ��ȣ�����û������������
	public int getWaitTasknumber() {
		return taskQueue.size();
	}

	// ����toString�����������̳߳���Ϣ�������̸߳�����������������
	@Override
	public String toString() {
		return "WorkThread number:" + COUNT + " and  finished task number:"
				+ FINISH_COUNT + "  wait task number:" + getWaitTasknumber();
	}

	private class WorkThread extends Thread {//�ڲ��� �����߳�
		
		private boolean isRunning = true;// �ù����߳��Ƿ���Ч�����ڽ����ù����߳�

		@Override
		public void run() {
			Runnable r = null;
			while (isRunning) {// ע�⣬���߳���Ч����Ȼ����run���������߳̾�û����
				synchronized (taskQueue) {
					while (isRunning && taskQueue.isEmpty()) {
						try {
							taskQueue.wait(20);//�ؼ����ڰ������������в��գ���ȡ������ִ�У���������пգ���ȴ�
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (!taskQueue.isEmpty())
						r = taskQueue.remove(0);// ȡ������
				}
				if (r != null) {
					r.run();// ִ������
					FINISH_COUNT++;//������+1
				}
				r = null;
			}
		}

		public void stopWorker() {
			// ֹͣ�������ø��߳���Ȼִ���굱ǰ�������Ȼ����
			isRunning = false;
		}
	}
}