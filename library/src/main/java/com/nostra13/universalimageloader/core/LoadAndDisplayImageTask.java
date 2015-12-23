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

import android.graphics.Bitmap;
import android.os.Handler;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.decode.ImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecodingInfo;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.utils.IoUtils;
import com.nostra13.universalimageloader.utils.L;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 图片加载和显示任务线程。 使用网络下载的或者本地文件系统缓存中图片。
 * 这边采用DisplayBitmapTak任务线程来进行显示图片
 * Presents load'n'display image task. Used to load image from Internet or file system, decode it to {@link Bitmap}, and
 * display it in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware} using {@link DisplayBitmapTask}.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoaderConfiguration
 * @see ImageLoadingInfo
 * @since 1.3.1
 */
final class LoadAndDisplayImageTask implements Runnable, IoUtils.CopyListener {

	private static final String LOG_WAITING_FOR_RESUME = "ImageLoader is paused. Waiting...  [%s]";
	private static final String LOG_RESUME_AFTER_PAUSE = ".. Resume loading [%s]";
	private static final String LOG_DELAY_BEFORE_LOADING = "Delay %d ms before loading...  [%s]";
	private static final String LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]";
	private static final String LOG_WAITING_FOR_IMAGE_LOADED = "Image already is loading. Waiting... [%s]";
	private static final String LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING = "...Get cached bitmap from memory after waiting. [%s]";
	private static final String LOG_LOAD_IMAGE_FROM_NETWORK = "Load image from network [%s]";
	private static final String LOG_LOAD_IMAGE_FROM_DISK_CACHE = "Load image from disk cache [%s]";
	private static final String LOG_RESIZE_CACHED_IMAGE_FILE = "Resize image in disk cache [%s]";
	private static final String LOG_PREPROCESS_IMAGE = "PreProcess image before caching in memory [%s]";
	private static final String LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]";
	private static final String LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]";
	private static final String LOG_CACHE_IMAGE_ON_DISK = "Cache image on disk [%s]";
	private static final String LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK = "Process image before cache on disk [%s]";
	private static final String LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]";
	private static final String LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]";
	private static final String LOG_TASK_INTERRUPTED = "Task was interrupted [%s]";

	private static final String ERROR_NO_IMAGE_STREAM = "No stream for image [%s]";
	private static final String ERROR_PRE_PROCESSOR_NULL = "Pre-processor returned null [%s]";
	private static final String ERROR_POST_PROCESSOR_NULL = "Post-processor returned null [%s]";
	private static final String ERROR_PROCESSOR_FOR_DISK_CACHE_NULL = "Bitmap processor for disk cache returned null [%s]";
    /*图片加载引擎*/
	private final ImageLoaderEngine engine;
	/*图片加载相关信息对象*/
	private final ImageLoadingInfo imageLoadingInfo;
	private final Handler handler;

	// Helper references  帮助项配置
	/*图片加载器配置信息*/
	private final ImageLoaderConfiguration configuration;
	private final ImageDownloader downloader;
	private final ImageDownloader networkDeniedDownloader;
	private final ImageDownloader slowNetworkDownloader;
	//图片解码器
	private final ImageDecoder decoder;
	final String uri;
	private final String memoryCacheKey;
	final ImageAware imageAware;
	private final ImageSize targetSize;
	final DisplayImageOptions options;
	final ImageLoadingListener listener;
	final ImageLoadingProgressListener progressListener;
	private final boolean syncLoading;

	// State vars  图片来源信息   默认为来源网络
	private LoadedFrom loadedFrom = LoadedFrom.NETWORK;

	/**
	 * 图片加载和显示任务构造方法
	 * @param engine                 图片加载引擎
	 * @param imageLoadingInfo       图片加载封装相关信息对象
	 * @param handler
	 */
	public LoadAndDisplayImageTask(ImageLoaderEngine engine, ImageLoadingInfo imageLoadingInfo, Handler handler) {
		this.engine = engine;
		this.imageLoadingInfo = imageLoadingInfo;
		this.handler = handler;

		configuration = engine.configuration;
		downloader = configuration.downloader;
		networkDeniedDownloader = configuration.networkDeniedDownloader;
		slowNetworkDownloader = configuration.slowNetworkDownloader;
		decoder = configuration.decoder;
		uri = imageLoadingInfo.uri;
		memoryCacheKey = imageLoadingInfo.memoryCacheKey;
		imageAware = imageLoadingInfo.imageAware;
		targetSize = imageLoadingInfo.targetSize;
		options = imageLoadingInfo.options;
		listener = imageLoadingInfo.listener;
		progressListener = imageLoadingInfo.progressListener;
		syncLoading = options.isSyncLoading();
	}

	@Override
	public void run() {
		//如果当前状态是暂停 当前任务直接返回
		if (waitIfPaused()) return;
		//如果当前状态需要等待  当前任务直接返回
		if (delayIfNeed()) return;

		ReentrantLock loadFromUriLock = imageLoadingInfo.loadFromUriLock;
		L.d(LOG_START_DISPLAY_IMAGE_TASK, memoryCacheKey);
		if (loadFromUriLock.isLocked()) {
			L.d(LOG_WAITING_FOR_IMAGE_LOADED, memoryCacheKey);
		}
        //任务加锁
		loadFromUriLock.lock();
		Bitmap bmp;
		try {
			//进行检查任务  判断当前要显示的引用对象是否已经被回收了
			checkTaskNotActual();
            //先从缓存中获取图片
			bmp = configuration.memoryCache.get(memoryCacheKey);
			if (bmp == null || bmp.isRecycled()) {
				//进行尝试获取加载图片（去文件中，文件中不存在去网络下载，然后缓存到文件）
				bmp = tryLoadBitmap();
				if (bmp == null) return; // listener callback already was fired

				checkTaskNotActual();
				checkTaskInterrupted();

				if (options.shouldPreProcess()) {
					L.d(LOG_PREPROCESS_IMAGE, memoryCacheKey);
					bmp = options.getPreProcessor().process(bmp);
					if (bmp == null) {
						L.e(ERROR_PRE_PROCESSOR_NULL, memoryCacheKey);
					}
				}

				if (bmp != null && options.isCacheInMemory()) {
					L.d(LOG_CACHE_IMAGE_IN_MEMORY, memoryCacheKey);
					configuration.memoryCache.put(memoryCacheKey, bmp);
				}
			} else {
				//从缓存中获取到图片信息
				//设置图片来源信息 --Memory Cache
				loadedFrom = LoadedFrom.MEMORY_CACHE;
				L.d(LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING, memoryCacheKey);
			}

			if (bmp != null && options.shouldPostProcess()) {
				L.d(LOG_POSTPROCESS_IMAGE, memoryCacheKey);
				bmp = options.getPostProcessor().process(bmp);
				if (bmp == null) {
					L.e(ERROR_POST_PROCESSOR_NULL, memoryCacheKey);
				}
			}
			checkTaskNotActual();
			checkTaskInterrupted();
		} catch (TaskCancelledException e) {
			fireCancelEvent();
			return;
		} finally {
			//任务取消锁
			loadFromUriLock.unlock();
		}
		//封装图片显示任务对象
		DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bmp, imageLoadingInfo, engine, loadedFrom);
		//进行任务运行
		runTask(displayBitmapTask, syncLoading, handler, engine);
	}

	/**
	 * @return <b>true</b> - if task should be interrupted; <b>false</b> - otherwise
	 * 判断是否需要等待暂时
	 */
	private boolean waitIfPaused() {
		AtomicBoolean pause = engine.getPause();
		if (pause.get()) {
			synchronized (engine.getPauseLock()) {
				if (pause.get()) {
					L.d(LOG_WAITING_FOR_RESUME, memoryCacheKey);
					try {
						engine.getPauseLock().wait();
					} catch (InterruptedException e) {
						L.e(LOG_TASK_INTERRUPTED, memoryCacheKey);
						return true;
					}
					L.d(LOG_RESUME_AFTER_PAUSE, memoryCacheKey);
				}
			}
		}
		return isTaskNotActual();
	}

	/**
	 * @return <b>true</b> - if task should be interrupted; <b>false</b> - otherwise
	 * 判断是否需要延时
	 */
	private boolean delayIfNeed() {
		if (options.shouldDelayBeforeLoading()) {
			L.d(LOG_DELAY_BEFORE_LOADING, options.getDelayBeforeLoading(), memoryCacheKey);
			try {
				Thread.sleep(options.getDelayBeforeLoading());
			} catch (InterruptedException e) {
				L.e(LOG_TASK_INTERRUPTED, memoryCacheKey);
				return true;
			}
			return isTaskNotActual();
		}
		return false;
	}

	/**
	 * 进行尝试获取加载图片
	 * @return
	 * @throws TaskCancelledException  抛出任务被取消异常
	 */
	private Bitmap tryLoadBitmap() throws TaskCancelledException {
		Bitmap bitmap = null;
		try {
			//从本地文件缓存中获取图片
			File imageFile = configuration.diskCache.get(uri);
			if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
				L.d(LOG_LOAD_IMAGE_FROM_DISK_CACHE, memoryCacheKey);
				//文件存在设置图片来源
				loadedFrom = LoadedFrom.DISC_CACHE;
                //检查引用是否已经被回收了
				checkTaskNotActual();
                //图片解码，文件转换成bitmap对象
				bitmap = decodeImage(Scheme.FILE.wrap(imageFile.getAbsolutePath()));
			}
			if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
				L.d(LOG_LOAD_IMAGE_FROM_NETWORK, memoryCacheKey);
				//本地文件系统中图片解码失败，尝试通过网络获取
				loadedFrom = LoadedFrom.NETWORK;
				String imageUriForDecoding = uri;
				//判断图片可以本地文件系统缓存以及尝试本地文本系统缓存(网络下载图片,下载成功图片缓存本地文件系统)
				if (options.isCacheOnDisk() && tryCacheImageOnDisk()) {
					//从本地文件系统缓存中获取图片
					imageFile = configuration.diskCache.get(uri);
					if (imageFile != null) {
						imageUriForDecoding = Scheme.FILE.wrap(imageFile.getAbsolutePath());
					}
				}

				checkTaskNotActual();
				//图片解码
				bitmap = decodeImage(imageUriForDecoding);

				if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
					//回调图片解码失败
					fireFailEvent(FailType.DECODING_ERROR, null);
				}
			}
		} catch (IllegalStateException e) {
			fireFailEvent(FailType.NETWORK_DENIED, null);
		} catch (TaskCancelledException e) {
			throw e;
		} catch (IOException e) {
			L.e(e);
			fireFailEvent(FailType.IO_ERROR, e);
		} catch (OutOfMemoryError e) {
			L.e(e);
			fireFailEvent(FailType.OUT_OF_MEMORY, e);
		} catch (Throwable e) {
			L.e(e);
			fireFailEvent(FailType.UNKNOWN, e);
		}
		//图片存在 返回
		return bitmap;
	}

	/**
	 * 根据图片资源地址进行解码图片
	 * @param imageUri   图片资源地址
	 * @return
	 * @throws IOException
	 */
	private Bitmap decodeImage(String imageUri) throws IOException {
		ViewScaleType viewScaleType = imageAware.getScaleType();
		ImageDecodingInfo decodingInfo = new ImageDecodingInfo(memoryCacheKey, imageUri, uri, targetSize, viewScaleType,
				getDownloader(), options);
		return decoder.decode(decodingInfo);
	}

	/**
	 *
 	 * 图片下载 并且保存本地，图片压缩
	 * @return <b>true</b> - if image was downloaded successfully; <b>false</b> - otherwise
	 */
	private boolean tryCacheImageOnDisk() throws TaskCancelledException {
		L.d(LOG_CACHE_IMAGE_ON_DISK, memoryCacheKey);
		boolean loaded;
		try {
			//图片下载并且保存本地
			loaded = downloadImage();
			if (loaded) {
				int width = configuration.maxImageWidthForDiskCache;
				int height = configuration.maxImageHeightForDiskCache;
				if (width > 0 || height > 0) {
					L.d(LOG_RESIZE_CACHED_IMAGE_FILE, memoryCacheKey);
					//根据尺寸大小配置 进行图片缩放和保存
					resizeAndSaveImage(width, height); // TODO : process boolean result
				}
			}
		} catch (IOException e) {
			L.e(e);
			loaded = false;
		}
		return loaded;
	}

	/**
	 * 图片通过网络下载,并且下载成功之后 进行本地文件系统缓存
	 * @return         下载并且缓存本地文件系统成功返回 true，否则返回false
	 * @throws IOException
	 */
	private boolean downloadImage() throws IOException {
		InputStream is = getDownloader().getStream(uri, options.getExtraForDownloader());
		if (is == null) {
			L.e(ERROR_NO_IMAGE_STREAM, memoryCacheKey);
			return false;
		} else {
			try {
				//进行图片资源缓存到本地文件系统中
				return configuration.diskCache.save(uri, is, this);
			} finally {
				IoUtils.closeSilently(is);
			}
		}
	}

	/**
	 * Decodes image file into Bitmap, resize it and save it back
	 * 解码图片 进行图片尺寸修改，然后保存
	 */
	private boolean resizeAndSaveImage(int maxWidth, int maxHeight) throws IOException {
		// Decode image file, compress and re-save it
		boolean saved = false;
		File targetFile = configuration.diskCache.get(uri);
		if (targetFile != null && targetFile.exists()) {
			//构建图片尺寸size对象
			ImageSize targetImageSize = new ImageSize(maxWidth, maxHeight);
			//图片显示配置参数构建
			DisplayImageOptions specialOptions = new DisplayImageOptions.Builder().cloneFrom(options)
					.imageScaleType(ImageScaleType.IN_SAMPLE_INT).build();
			ImageDecodingInfo decodingInfo = new ImageDecodingInfo(memoryCacheKey,
					Scheme.FILE.wrap(targetFile.getAbsolutePath()), uri, targetImageSize, ViewScaleType.FIT_INSIDE,
					getDownloader(), specialOptions);
			//获取解码之后的图片
			Bitmap bmp = decoder.decode(decodingInfo);
			if (bmp != null && configuration.processorForDiskCache != null) {
				L.d(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK, memoryCacheKey);
				bmp = configuration.processorForDiskCache.process(bmp);
				if (bmp == null) {
					L.e(ERROR_PROCESSOR_FOR_DISK_CACHE_NULL, memoryCacheKey);
				}
			}
			if (bmp != null) {
				//图片重新保存本地文件系统
				saved = configuration.diskCache.save(uri, bmp);
				bmp.recycle();
			}
		}
		return saved;
	}

	/**
	 * 字节流正在拷贝的时候调用
	 * @param current Loaded bytes
	 * @param total   Total bytes for loading
	 * @return
	 */
	@Override
	public boolean onBytesCopied(int current, int total) {
		return syncLoading || fireProgressEvent(current, total);
	}

	/** @return <b>true</b> - if loading should be continued; <b>false</b> - if loading should be interrupted */
	/**
	 * 进行图片进度事件
	 * @param current  当前图片下载的图片资源大小
	 * @param total    图片资源下载的总大小
	 * @return
	 */
	private boolean fireProgressEvent(final int current, final int total) {
		if (isTaskInterrupted() || isTaskNotActual()) return false;
		if (progressListener != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					//进行回调图片加载进度信息
					progressListener.onProgressUpdate(uri, imageAware.getWrappedView(), current, total);
				}
			};
			//执行任务
			runTask(r, false, handler, engine);
		}
		return true;
	}

	/**
	 * 图片加载失败事件处理分发
	 * @param failType
	 * @param failCause
	 */
	private void fireFailEvent(final FailType failType, final Throwable failCause) {
		if (syncLoading || isTaskInterrupted() || isTaskNotActual()) return;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (options.shouldShowImageOnFail()) {
					//存在图片加载失败的占位图片配置，进行设置
					imageAware.setImageDrawable(options.getImageOnFail(configuration.resources));
				}
				//进行回调图片加载失败方法
				listener.onLoadingFailed(uri, imageAware.getWrappedView(), new FailReason(failType, failCause));
			}
		};
		//任务进行运行
		runTask(r, false, handler, engine);
	}

	/**
	 * 取消事件处理
	 */
	private void fireCancelEvent() {
		if (syncLoading || isTaskInterrupted()) return;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				/**
				 * 进行回调图片加载取消的方法
				 */
				listener.onLoadingCancelled(uri, imageAware.getWrappedView());
			}
		};
		//任务执行
		runTask(r, false, handler, engine);
	}

	/**
	 * 获取下载加载器
	 * @return
	 */
	private ImageDownloader getDownloader() {
		ImageDownloader d;
		if (engine.isNetworkDenied()) {
			d = networkDeniedDownloader;
		} else if (engine.isSlowNetwork()) {
			d = slowNetworkDownloader;
		} else {
			d = downloader;
		}
		return d;
	}

	/**
	 * @throws TaskCancelledException if task is not actual (target ImageAware is collected by GC or the image URI of
	 *                                this task doesn't match to image URI which is actual for current ImageAware at
	 *                                this moment)
	 */
	private void checkTaskNotActual() throws TaskCancelledException {
		checkViewCollected();
		checkViewReused();
	}

	/**
	 * @return <b>true</b> - if task is not actual (target ImageAware is collected by GC or the image URI of this task
	 * doesn't match to image URI which is actual for current ImageAware at this moment)); <b>false</b> - otherwise
	 */
	private boolean isTaskNotActual() {
		return isViewCollected() || isViewReused();
	}

	/** @throws TaskCancelledException if target ImageAware is collected */
	private void checkViewCollected() throws TaskCancelledException {
		if (isViewCollected()) {
			throw new TaskCancelledException();
		}
	}

	/**
	 * @return <b>true</b> - if target ImageAware is collected by GC; <b>false</b> - otherwise
	 * 判断ImageAware是否已经被GC回收
	 */
	private boolean isViewCollected() {
		if (imageAware.isCollected()) {
			L.d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey);
			return true;
		}
		return false;
	}

	/** @throws TaskCancelledException if target ImageAware is collected by GC */
	private void checkViewReused() throws TaskCancelledException {
		if (isViewReused()) {
			throw new TaskCancelledException();
		}
	}

	/** @return <b>true</b> - if current ImageAware is reused for displaying another image; <b>false</b> - otherwise */
	private boolean isViewReused() {
		String currentCacheKey = engine.getLoadingUriForView(imageAware);
		// Check whether memory cache key (image URI) for current ImageAware is actual.
		// If ImageAware is reused for another task then current task should be cancelled.
		boolean imageAwareWasReused = !memoryCacheKey.equals(currentCacheKey);
		if (imageAwareWasReused) {
			L.d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey);
			return true;
		}
		return false;
	}

	/** @throws TaskCancelledException if current task was interrupted */
	private void checkTaskInterrupted() throws TaskCancelledException {
		if (isTaskInterrupted()) {
			throw new TaskCancelledException();
		}
	}

	/** @return <b>true</b> - if current task was interrupted; <b>false</b> - otherwise */
	private boolean isTaskInterrupted() {
		if (Thread.interrupted()) {
			L.d(LOG_TASK_INTERRUPTED, memoryCacheKey);
			return true;
		}
		return false;
	}

	String getLoadingUri() {
		return uri;
	}

	/**
	 * 任务运行方法
	 * @param r           任务运行线程
	 * @param sync        是否同步标志
	 * @param handler     任务分发处理
	 * @param engine      图片加载引擎
	 */
	static void runTask(Runnable r, boolean sync, Handler handler, ImageLoaderEngine engine) {
		if (sync) {
			//如果同步 任务直接运行
			r.run();
		} else if (handler == null) {
			engine.fireCallback(r);
		} else {
			//任务通过Handler分发到主线程执行
			handler.post(r);
		}
	}

	/**
	 * Exceptions for case when task is cancelled (thread is interrupted, image view is reused for another task, view is
	 * collected by GC).
	 * 自定义任务取消异常
	 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
	 * @since 1.9.1
	 */
	class TaskCancelledException extends Exception {
	}
}
