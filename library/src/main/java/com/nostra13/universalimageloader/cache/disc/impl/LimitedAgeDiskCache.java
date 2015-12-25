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
 * 缓存时间限制，如果文件超过最大缓存事件，会被删除。  缓存容量无限制。
 * 当前类实现BaseDiskCache抽象类，进行功能扩展
 * Cache which deletes files which were loaded more than defined time. Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
public class LimitedAgeDiskCache extends BaseDiskCache {

	private final long maxFileAge;
    //维护文件和文件修改的时间
	private final Map<File, Long> loadingDates = Collections.synchronizedMap(new HashMap<File, Long>());

	/**
	 * 构造器  需要传入文件的最大缓存时间
	 * @param cacheDir Directory for file caching
	 * @param maxAge   时间的单位为秒级别.
	 *                 Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                 treatment (and therefore be reloaded).
	 */
	public LimitedAgeDiskCache(File cacheDir, long maxAge) {
		this(cacheDir, null, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
	}

	/**
	 * 构造器   需要传入缓存文件夹，备用缓存文件夹，以及最大的缓存时间
	 * @param cacheDir Directory for file caching
	 * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                 treatment (and therefore be reloaded).
	 */
	public LimitedAgeDiskCache(File cacheDir, File reserveCacheDir, long maxAge) {
		this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
	}

	/**
	 *
	 * @param cacheDir          Directory for file caching
	 * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 * @param fileNameGenerator Name generator for cached files
	 * @param maxAge            Max file age (in seconds). If file age will exceed this value then it'll be removed on next
	 *                          treatment (and therefore be reloaded).
	 */
	/**
	 * 构造器
	 * @param cacheDir             缓存文件夹
	 * @param reserveCacheDir      备用缓存文件夹
	 * @param fileNameGenerator    缓存文件名 命名贵方
	 * @param maxAge               文件最大缓存事件
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
            //根据时间间隔进行判断 是否需要删除文件
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
		//时间保存
		rememberUsage(imageUri);
		return saved;
	}

	/**
	 * 进行文件保存缓存  同时文件修改时间维护
	 * @param imageUri
	 * @param bitmap
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
		boolean saved = super.save(imageUri, bitmap);
		//时间维护
		rememberUsage(imageUri);
		return saved;
	}

	/**
	 * 根据图片URL地址进行删除缓存中的文件，同时删除维护的map对象
	 * @param imageUri Image URI
	 * @return
	 */
	@Override
	public boolean remove(String imageUri) {
		loadingDates.remove(getFile(imageUri));
		return super.remove(imageUri);
	}

	/**
	 * 清除缓存文件 同时清除维护的map对象
	 */
	@Override
	public void clear() {
		super.clear();
		loadingDates.clear();
	}

	/**
	 *
	 * @param imageUri
	 */
	private void rememberUsage(String imageUri) {
		File file = getFile(imageUri);
		long currentTime = System.currentTimeMillis();
		//修改文件最后修改事件
		file.setLastModified(currentTime);
		//保存文件修改事件
		loadingDates.put(file, currentTime);
	}
}