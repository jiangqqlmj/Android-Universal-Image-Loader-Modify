/*******************************************************************************
 * Copyright 2011-2014 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.nostra13.universalimageloader.core;

import android.view.View;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * 图片加载引擎，用于执行图片加载和显示任务
 * {@link ImageLoader} engine which responsible for {@linkplain LoadAndDisplayImageTask display task} execution.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.7.1
 */
class ImageLoaderEngine {
	/*ImageLoader加载配置*/
	final ImageLoaderConfiguration configuration;
	/*任务执行者*/
	private Executor taskExecutor;
	/*图片缓存任务执行则*/
	private Executor taskExecutorForCachedImages;
	/*任务分配者*/
	private Executor taskDistributor;

	private final Map<Integer, String> cacheKeysForImageAwares = Collections
			.synchronizedMap(new HashMap<Integer, String>());
	private final Map<String, ReentrantLock> uriLocks = new WeakHashMap<String, ReentrantLock>();
	/*暂停*/
	private final AtomicBoolean paused = new AtomicBoolean(false);
	/*网络拒绝访问*/
	private final AtomicBoolean networkDenied = new AtomicBoolean(false);
	/*网络慢*/
	private final AtomicBoolean slowNetwork = new AtomicBoolean(false);

	private final Object pauseLock = new Object();

	/**
	 * ImageLoader引擎构造器
	 * @param configuration
	 */
	ImageLoaderEngine(ImageLoaderConfiguration configuration) {
		//初始化ImageLoader配置参数
		this.configuration = configuration;
        //初始化三个不同任务的执行者
		taskExecutor = configuration.taskExecutor;
		taskExecutorForCachedImages = configuration.taskExecutorForCachedImages;
		taskDistributor = DefaultConfigurationFactory.createTaskDistributor();
	}

	/** Submits task to execution pool */
	/**
	 * 提交图片加载和显示任务到执行线程池中,进行运行
	 * @param task   具体需要执行的任务
	 */
	void submit(final LoadAndDisplayImageTask task) {
		taskDistributor.execute(new Runnable() {
			@Override
			public void run() {
				//从文件系统缓存中获取图片文件
				File image = configuration.diskCache.get(task.getLoadingUri());
				//判断是否已经取得了图片
				boolean isImageCachedOnDisk = image != null && image.exists();
				initExecutorsIfNeed();
				if (isImageCachedOnDisk) {
					//如果当前图片已经缓存在本地文件系统了，直接采用taskExecutorForCachedImages来进行执行任务
					taskExecutorForCachedImages.execute(task);
				} else {
					//当天图片在本地文件系统中没有缓存，直接采用taskExecutor来进行执行任务
					taskExecutor.execute(task);
				}
			}
		});
	}

	/**
	 * Submits task to execution pool
	 * 提交图片显示任务并且执行 (该图片从内存缓存中取得)
	 */
	void submit(ProcessAndDisplayImageTask task) {
		initExecutorsIfNeed();
		taskExecutorForCachedImages.execute(task);
	}

	/**
	 * 根据需要进行初始化执行者
	 */
	private void initExecutorsIfNeed() {
		if (!configuration.customExecutor && ((ExecutorService) taskExecutor).isShutdown()) {
			taskExecutor = createTaskExecutor();
		}
		if (!configuration.customExecutorForCachedImages && ((ExecutorService) taskExecutorForCachedImages)
				.isShutdown()) {
			taskExecutorForCachedImages = createTaskExecutor();
		}
	}

	/**
	 * 进行创建任务执行者
	 * @return
	 */
	private Executor createTaskExecutor() {
		return DefaultConfigurationFactory
				.createExecutor(configuration.threadPoolSize, configuration.threadPriority,
				configuration.tasksProcessingType);
	}

	/**
	 * 获取当前被加载ImageAware到图片的地址
	 * Returns URI of image which is loading at this moment into passed {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}
	 */
	String getLoadingUriForView(ImageAware imageAware) {
		return cacheKeysForImageAwares.get(imageAware.getId());
	}

	/**
	 *
	 * Associates <b>memoryCacheKey</b> with <b>imageAware</b>. Then it helps to define image URI is loaded into View at
	 * exact moment.
	 */
	void prepareDisplayTaskFor(ImageAware imageAware, String memoryCacheKey) {
		cacheKeysForImageAwares.put(imageAware.getId(), memoryCacheKey);
	}

	/**
	 * Cancels the task of loading and displaying image for incoming <b>imageAware</b>.
	 *
	 * @param imageAware {@link com.nostra13.universalimageloader.core.imageaware.ImageAware} for which display task
	 *                   will be cancelled
	 */
	void cancelDisplayTaskFor(ImageAware imageAware) {
		cacheKeysForImageAwares.remove(imageAware.getId());
	}

	/**
	 * Denies or allows engine to download images from the network.<br /> <br /> If downloads are denied and if image
	 * isn't cached then {@link ImageLoadingListener#onLoadingFailed(String, View, FailReason)} callback will be fired
	 * with {@link FailReason.FailType#NETWORK_DENIED}
	 *
	 * @param denyNetworkDownloads pass <b>true</b> - to deny engine to download images from the network; <b>false</b> -
	 *                             to allow engine to download images from network.
	 */
	void denyNetworkDownloads(boolean denyNetworkDownloads) {
		networkDenied.set(denyNetworkDownloads);
	}

	/**
	 * Sets option whether ImageLoader will use {@link FlushedInputStream} for network downloads to handle <a
	 * href="http://code.google.com/p/android/issues/detail?id=6066">this known problem</a> or not.
	 *
	 * @param handleSlowNetwork pass <b>true</b> - to use {@link FlushedInputStream} for network downloads; <b>false</b>
	 *                          - otherwise.
	 */
	void handleSlowNetwork(boolean handleSlowNetwork) {
		slowNetwork.set(handleSlowNetwork);
	}

	/**
	 * Pauses engine. All new "load&display" tasks won't be executed until ImageLoader is {@link #resume() resumed}.<br
	 * /> Already running tasks are not paused.
	 * 暂停任务运行
	 */
	void pause() {
		paused.set(true);
	}

	/**
	 * Resumes engine work. Paused "load&display" tasks will continue its work.
	 * 任务恢复运行
	 */
	void resume() {
		paused.set(false);
		synchronized (pauseLock) {
			pauseLock.notifyAll();
		}
	}

	/**
	 * 停止ImageLoader引擎,取消所有正在运行或者挂起的图片显示任务，并且清除内部的数据
	 * Stops engine, cancels all running and scheduled display image tasks. Clears internal data.
	 * <br />
	 * <b>NOTE:</b> This method doesn't shutdown
	 * {@linkplain com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder#taskExecutor(java.util.concurrent.Executor)
	 * custom task executors} if you set them.
	 */
	void stop() {
		if (!configuration.customExecutor) {
			((ExecutorService) taskExecutor).shutdownNow();
		}
		if (!configuration.customExecutorForCachedImages) {
			((ExecutorService) taskExecutorForCachedImages).shutdownNow();
		}
		cacheKeysForImageAwares.clear();
		uriLocks.clear();
	}

	void fireCallback(Runnable r) {
		taskDistributor.execute(r);
	}

	ReentrantLock getLockForUri(String uri) {
		ReentrantLock lock = uriLocks.get(uri);
		if (lock == null) {
			lock = new ReentrantLock();
			uriLocks.put(uri, lock);
		}
		return lock;
	}

	AtomicBoolean getPause() {
		return paused;
	}

	Object getPauseLock() {
		return pauseLock;
	}

	boolean isNetworkDenied() {
		return networkDenied.get();
	}

	boolean isSlowNetwork() {
		return slowNetwork.get();
	}
}
