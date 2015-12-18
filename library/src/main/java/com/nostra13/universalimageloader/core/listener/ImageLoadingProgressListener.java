/*******************************************************************************
 * Copyright 2013 Sergey Tarasevich
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

import android.view.View;

/**
 * 图片加载(下载)进度回调
 * Listener for image loading progress.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.1
 */
public interface ImageLoadingProgressListener {

	/**
	 * 当图片下载进度发生变化的时候进行回调
	 * Is called when image loading progress changed.
	 *
	 * @param imageUri Image URI             图片的URL地址
	 * @param view     View for image. Can be <b>null</b>.   显示图片的控件View
	 * @param current  Downloaded size in bytes       图片已经下载的大小
	 * @param total    Total size in bytes            图片总共的大小
	 */
	void onProgressUpdate(String imageUri, View view, int current, int total);
}
