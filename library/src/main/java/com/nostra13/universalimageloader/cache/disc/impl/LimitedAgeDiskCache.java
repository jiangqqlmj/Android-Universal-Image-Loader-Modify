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
package com.nostra13.universalimageloader.cache.disc.impl;

import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache which deletes files which were loaded more than defined time. Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
public class LimitedAgeDiskCache extends BaseDiskCache {

	private final long maxFileAge;

	private final Map<File, Long> loadingDates = Collections.synchronizedMap(new HashMap<File, Long>());

	/**
	 * @param cacheDir Directory for file caching
	 * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                 treatment (and therefore be reloaded).
	 */
	public LimitedAgeDiskCache(File cacheDir, long maxAge) {
		this(cacheDir, null, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
	}

	/**
	 * @param cacheDir Directory for file caching
	 * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                 treatment (and therefore be reloaded).
	 */
	public LimitedAgeDiskCache(File cacheDir, File reserveCacheDir, long maxAge) {
		this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
	}

	/**
	 * @param cacheDir          Directory for file caching
	 * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 * @param fileNameGenerator Name generator for cached files
	 * @param maxAge            Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                          treatment (and therefore be reloaded).
	 */
	public LimitedAgeDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator, long maxAge) {
		super(cacheDir, reserveCacheDir, fileNameGenerator);
		this.maxFileAge = maxAge * 1000; // to milliseconds
	}

	/**
	 * 从缓存中获取缓存图片，同时进行修改时间和缓存的事件进行比较，超过最大缓存时间之后进行删除本地缓存图片
	 * @param imageUri   根据图片的URL地址
	 * @return
	 */
	@Override
	public File get(String imageUri) {
		File file = super.get(imageUri);
		if (file != null && file.exists()) {
			boolean cached;
			Long loadingDate = loadingDates.get(file);
			if (loadingDate == null) {
				cached = false;
				loadingDate = file.lastModified();
			} else {
				cached = true;
			}

			if (System.currentTimeMillis() - loadingDate > maxFileAge) {
				file.delete();
				loadingDates.remove(file);
			} else if (!cached) {
				loadingDates.put(file, loadingDate);
			}
		}
		return file;
	}

	/**
	 * 进行保存图片到本地缓存路径中  --使用图片流
	 * @param imageUri       图片的URL地址
	 * @param imageStream    图片流
	 * @param listener       图片流拷贝完成进度回调接口
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
		boolean saved = super.save(imageUri, imageStream, listener);
		rememberUsage(imageUri);
		return saved;
	}

	/**
	 *
	 * @param imageUri
	 * @param bitmap
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
		boolean saved = super.save(imageUri, bitmap);
		rememberUsage(imageUri);
		return saved;
	}

	@Override
	public boolean remove(String imageUri) {
		loadingDates.remove(getFile(imageUri));
		return super.remove(imageUri);
	}

	@Override
	public void clear() {
		super.clear();
		loadingDates.clear();
	}

	private void rememberUsage(String imageUri) {
		File file = getFile(imageUri);
		long currentTime = System.currentTimeMillis();
		file.setLastModified(currentTime);
		loadingDates.put(file, currentTime);
	}
}