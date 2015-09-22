package com.sky;

import java.util.concurrent.CountDownLatch;

/*
 * ≤‚ ‘¿‡
 * */
public class Test {
	private static ThreadPool mPool = null;
	private static volatile int Count = 0;
	private static CountDownLatch latch = new CountDownLatch(50);

	public static void main(String[] args) {
		mPool = ThreadPool.getThreadPool(10);

		Runnable[] r = new Runnable[50];

		for (int i = 0; i < 50; i++) {
			final int time = i * 5;
			r[i] = new Runnable() {
				@Override
				public void run() {
					Count++;
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					latch.countDown();
				}
			};
		}

		mPool.addtask(r);

		try {
			latch.await();
			System.out.println(Count);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
