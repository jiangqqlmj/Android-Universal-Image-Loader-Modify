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
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.L;

/**
 * 图片显示任务线程，该必须在UI线程取消
 * Displays bitmap in {@link com.nostra13.universalimageloader.core.imageaware.ImageAware}. Must be called on UI thread.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoadingListener
 * @see BitmapDisplayer
 * @since 1.3.1
 */
final class DisplayBitmapTask implements Runnable {

	private static final String LOG_DISPLAY_IMAGE_IN_IMAGEAWARE = "Display image in ImageAware (loaded from %1$s) [%2$s]";
	private static final String LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]";
	private static final String LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]";
	/*下载成功的图片bitmap*/
	private final Bitmap bitmap;
	/*图片的URL地址*/
	private final String imageUri;
	/*显示图片的控件，ImageAware对象采用iamgeView包装*/
	private final ImageAware imageAware;
	/*缓存key*/
	private final String memoryCacheKey;
	/*图片显示器*/
	private final BitmapDisplayer displayer;
	/*图片加载监听器*/
	private final ImageLoadingListener listener;
	/*图片加载引擎*/
	private final ImageLoaderEngine engine;
	/*图片来源标志*/
	private final LoadedFrom loadedFrom;

	/**
	 * 图片显示任务  封装了图片加载显示相关信息
	 * @param bitmap                下载成功的图片
	 * @param imageLoadingInfo      图片加载相关信息
	 * @param engine                图片加载引擎
	 * @param loadedFrom            图片加载来源
	 */
	public DisplayBitmapTask(Bitmap bitmap, ImageLoadingInfo imageLoadingInfo, ImageLoaderEngine engine,
			LoadedFrom loadedFrom) {
		this.bitmap = bitmap;
		imageUri = imageLoadingInfo.uri;
		imageAware = imageLoadingInfo.imageAware;
		memoryCacheKey = imageLoadingInfo.memoryCacheKey;
		displayer = imageLoadingInfo.options.getDisplayer();
		listener = imageLoadingInfo.listener;
		this.engine = engine;
		this.loadedFrom = loadedFrom;
	}

	/**
	 * 线程任务执行方法
	 */
	@Override
	public void run() {
		//判断当前imageAware是否被回收
		if (imageAware.isCollected()) {
			L.d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey);
			//进行回调加载取消
			listener.onLoadingCancelled(imageUri, imageAware.getWrappedView());
		} else if (isViewWasReused()) {
			//判断对象是否为同一个
			L.d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey);
			listener.onLoadingCancelled(imageUri, imageAware.getWrappedView());
		} else {
			//进行显示
			L.d(LOG_DISPLAY_IMAGE_IN_IMAGEAWARE, loadedFrom, memoryCacheKey);
			//显示图片
			displayer.display(bitmap, imageAware, loadedFrom);
			engine.cancelDisplayTaskFor(imageAware);
			//图片显示完成回调
			listener.onLoadingComplete(imageUri, imageAware.getWrappedView(), bitmap);
		}
	}
	/**
	 * 检查缓存对象和当前ImageAware是否为同一个
	 *Checks whether memory cache key (image URI) for current ImageAware is actual
	 *
	 */
	private boolean isViewWasReused() {
		String currentCacheKey = engine.getLoadingUriForView(imageAware);
		return !memoryCacheKey.equals(currentCacheKey);
	}
}
