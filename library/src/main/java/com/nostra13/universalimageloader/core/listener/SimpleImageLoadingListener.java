/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
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
package com.nostra13.universalimageloader.core.listener;

import android.graphics.Bitmap;
import android.view.View;
import com.nostra13.universalimageloader.core.assist.FailReason;

/**
 * 该为一个简便的图片加载监听器，如果我们需要监听图片加载过程中的部分事件，就可以使用这个进行选择性实现
 * A convenient class to extend when you only want to listen for a subset of all the image loading events. This
 * implements all methods in the {@link com.nostra13.universalimageloader.core.listener.ImageLoadingListener} but does
 * nothing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
public class SimpleImageLoadingListener implements ImageLoadingListener {
	/**
	 * 图片加载开始回调
	 * @param imageUri Loading image URI
	 * @param view     View for image
	 */
	@Override
	public void onLoadingStarted(String imageUri, View view) {
		// Empty implementation
	}

	/**
	 * 图片加载失败回调
	 * @param imageUri   Loading image URI
	 * @param view       View for image. Can be <b>null</b>.
	 * @param failReason {@linkplain com.nostra13.universalimageloader.core.assist.FailReason The reason} why image
	 */
	@Override
	public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
		// Empty implementation
	}

	/**
	 * 图片加载完成回调
	 * @param imageUri    Loaded image URI
	 * @param view        View for image. Can be <b>null</b>.
	 * @param loadedImage Bitmap of loaded and decoded image
	 */
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		// Empty implementation
	}

	/**
	 * 图片加载取消回调
	 * @param imageUri Loading image URI
	 * @param view     View for image. Can be <b>null</b>.
	 */
	@Override
	public void onLoadingCancelled(String imageUri, View view) {
		// Empty implementation
	}
}
