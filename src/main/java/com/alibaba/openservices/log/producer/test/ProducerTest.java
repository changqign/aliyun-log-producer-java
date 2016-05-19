package com.alibaba.openservices.log.producer.test;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

import com.alibaba.openservices.log.producer.LogProducer;
import com.alibaba.openservices.log.producer.ProducerConfig;
import com.alibaba.openservices.log.producer.ProjectConfig;
import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;

public class ProducerTest {
	private final static int ThreadsCount = 20;
	public static String RandomString(int length) {
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int num = random.nextInt(62);
			buf.append(str.charAt(num));
		}
		return buf.toString();
	}

	public static void LowLevelPutLogsTest() {
		Client client = new Client("cn-shanghai-corp.sls.aliyuncs.com",
				"K59AYAGHpf8w9X0J", "7hvlClY49eO4v5Nm2EOkUOoIOlK3MW");
		Random random = new Random();
		final Vector<Vector<LogItem>> logGroups = new Vector<Vector<LogItem>>();
		for (int i = 0; i < 100; ++i) {
			Vector<LogItem> tmpLogGroup = new Vector<LogItem>();
			for (int j = 0; j < 4095; ++j) {
				LogItem logItem = new LogItem(
						(int) (new Date().getTime() / 1000));
				logItem.PushBack("level", "info" + System.currentTimeMillis());
				logItem.PushBack("message", "test producer send perf "
						+ RandomString(50));
				logItem.PushBack("method", "SenderToServer " + RandomString(10));
				tmpLogGroup.add(logItem);
			}
			logGroups.add(tmpLogGroup);
		}
		while (true) {
			try {
				client.PutLogs("ali-log-service", "test-producer", "",
						logGroups.get(random.nextInt(99)), null);
			} catch (LogException e) {
				e.printStackTrace();
			}
		}
	}

	public static void PutEveryLogsTest() throws InterruptedException {
		final Client client = new Client("cn-shanghai-corp.sls.aliyuncs.com",
				"K59AYAGHpf8w9X0J", "7hvlClY49eO4v5Nm2EOkUOoIOlK3MW");
		final Vector<Vector<LogItem>> logGroups = new Vector<Vector<LogItem>>();
		for (int i = 0; i < 1000000; ++i) {
			Vector<LogItem> tmpLogGroup = new Vector<LogItem>();
			LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
			logItem.PushBack("level", "info" + System.currentTimeMillis());
			logItem.PushBack("message", "test producer send perf "
					+ RandomString(50));
			logItem.PushBack("method", "SenderToServer " + RandomString(10));
			tmpLogGroup.add(logItem);
			logGroups.add(tmpLogGroup);
		}
		System.out.println("threads begin...");
		Thread[] threads = new Thread[ThreadsCount];
		for (int i = 0; i < ThreadsCount; ++i) {
			threads[i] = new Thread(null, new Runnable() {
				Random random = new Random();

				public void run() {
					while (true) {
						try {
							client.PutLogs("ali-log-service", "test-producer", "",
									logGroups.get(random.nextInt(1000000)), null);
						} catch (LogException e) {
							e.printStackTrace();
						}
					}
				}
			}, i + "");
			threads[i].start();
		}
		Thread.sleep(24 * 60 * 60 * 1000);
	}

	public static void ProducerTest() throws InterruptedException {
		ProducerConfig producerConfig = new ProducerConfig();
		final LogProducer producer = new LogProducer(producerConfig);
		producer.updateProjectConfig(new ProjectConfig("ali-log-service",
				"cn-shanghai-corp.sls.aliyuncs.com", "K59AYAGHpf8w9X0J",
				"7hvlClY49eO4v5Nm2EOkUOoIOlK3MW"));
		final Vector<Vector<LogItem>> logGroups = new Vector<Vector<LogItem>>();
		for (int i = 0; i < 100000; ++i) {
			Vector<LogItem> tmpLogGroup = new Vector<LogItem>();
			LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
			logItem.PushBack("level", "info" + System.currentTimeMillis());
			logItem.PushBack("message", "test producer send perf "
					+ RandomString(50));
			logItem.PushBack("method", "SenderToServer " + RandomString(10));
			tmpLogGroup.add(logItem);
			logGroups.add(tmpLogGroup);
		}
		System.out.println("threads begin...");
		Thread[] threads = new Thread[ThreadsCount];
		for (int i = 0; i < ThreadsCount; ++i) {
			threads[i] = new Thread(null, new Runnable() {
				Random random = new Random();

				public void run() {
					while (true) {
						producer.send("ali-log-service", "test-producer", "",
								null, logGroups.get(random.nextInt(99999)),
								new CallbackTest());
					}
				}
			}, i + "");
			threads[i].start();
		}
		Thread.sleep(24 * 60 * 60 * 1000);
	}

	public static void main(String args[]) throws InterruptedException {
		if (args[0].compareTo("sdk") == 0) {
			LowLevelPutLogsTest();
		} else if(args[0].compareTo("producer") == 0){
			ProducerTest();
		} else if(args[0].compareTo("any") == 0){
			PutEveryLogsTest();
		}
	}
}
