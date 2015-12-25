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
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.utils.IoUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件系统缓存(磁盘缓存)工具基础抽象类   该类已经实现了相关统一方法和扩展
 * Base disk cache.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see FileNameGenerator
 * @since 1.0.0
 */
public abstract class BaseDiskCache implements DiskCache {
	/**
	 * {@value}
	 * 默认缓存区大小
	 */
	public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32 Kb
	/**
	 * {@value}
	 * 默认图片压缩格式
	 */
	public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
	/**
	 * {@value}
	 * 默认图片压缩质量
	 */
	public static final int DEFAULT_COMPRESS_QUALITY = 100;

	private static final String ERROR_ARG_NULL = " argument must be not null";
	private static final String TEMP_IMAGE_POSTFIX = ".tmp";

	/*缓存文件夹*/
	protected final File cacheDir;
	/*备用缓存文件夹*/
	protected final File reserveCacheDir;
    /*文件名生成器*/
	protected final FileNameGenerator fileNameGenerator;
    /*缓冲区大小  已经赋值默认缓冲区大小*/
	protected int bufferSize = DEFAULT_BUFFER_SIZE;
	/*图片压缩格式  已经赋值默认压缩格式*/
	protected Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
	/*图片压缩质量 已经赋值默认压缩质量*/
	protected int compressQuality = DEFAULT_COMPRESS_QUALITY;

	/**
	 * 构造方法  缓存文件缓存的文件夹
	 * @param cacheDir Directory for file caching
	 */
	public BaseDiskCache(File cacheDir) {
		this(cacheDir, null);
	}

	/**
	 * 构造方法   传入缓存文件夹 以及备用缓存文件夹
	 * @param cacheDir        Directory for file caching
	 * @param reserveCacheDir null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 */
	public BaseDiskCache(File cacheDir, File reserveCacheDir) {
		this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator());
	}

	/**
	 * 构造方法   传入缓存文件夹 以及备用缓存文件夹，缓存文件命名方式
	 * @param cacheDir          Directory for file caching
	 * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 * @param fileNameGenerator {@linkplain com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
	 *                          Name generator} for cached files
	 */
	public BaseDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator) {
		if (cacheDir == null) {
			throw new IllegalArgumentException("cacheDir" + ERROR_ARG_NULL);
		}
		if (fileNameGenerator == null) {
			throw new IllegalArgumentException("fileNameGenerator" + ERROR_ARG_NULL);
		}

		this.cacheDir = cacheDir;
		this.reserveCacheDir = reserveCacheDir;
		this.fileNameGenerator = fileNameGenerator;
	}

	/**
	 * 获取缓存文件夹
	 * @return
	 */
	@Override
	public File getDirectory() {
		return cacheDir;
	}

	/**
	 * 获取缓存文件
	 * @param imageUri Original image URI
	 * @return
	 */
	@Override
	public File get(String imageUri) {
		return getFile(imageUri);
	}

	/**
	 * 根据文件流进行保存到缓存文件中
	 * @param imageUri    Original image URI    原图片URL地址作为生成缓存文件的文件名
	 * @param imageStream Input stream of image (shouldn't be closed in this method)   图片流
	 * @param listener     用于监听流保存本地的进度
	 * Listener for saving progress, can be ignored if you don't use
	 *                    {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
	 *                    progress listener} in ImageLoader calls
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
		File imageFile = getFile(imageUri);
		File tmpFile = new File(imageFile.getAbsolutePath() + TEMP_IMAGE_POSTFIX);
		boolean loaded = false;
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
			try {
				loaded = IoUtils.copyStream(imageStream, os, listener, bufferSize);
			} finally {
				IoUtils.closeSilently(os);
			}
		} finally {
			if (loaded && !tmpFile.renameTo(imageFile)) {
				loaded = false;
			}
			if (!loaded) {
				tmpFile.delete();
			}
		}
		return loaded;
	}

	/**
	 * 进行保存图片(bitmap)到缓存中 其中根据imageurl来生成缓存文件命名
	 * @param imageUri Original image URI
	 * @param bitmap   Image bitmap
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
		//根据图片链接地址 生成本地文件
		File imageFile = getFile(imageUri);
		//生成临时文件
		File tmpFile = new File(imageFile.getAbsolutePath() + TEMP_IMAGE_POSTFIX);
		//写入文件
		OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
		boolean savedSuccessfully = false;
		try {
			//进行图片压缩
			savedSuccessfully = bitmap.compress(compressFormat, compressQuality, os);
		} finally {
			IoUtils.closeSilently(os);
			if (savedSuccessfully && !tmpFile.renameTo(imageFile)) {
				savedSuccessfully = false;
			}
			if (!savedSuccessfully) {
				tmpFile.delete();
			}
		}
		bitmap.recycle();
		return savedSuccessfully;
	}

	/**
	 * 根据图片URL地址 进行删除指定的相关文件
	 * @param imageUri Image URI
	 * @return
	 */
	@Override
	public boolean remove(String imageUri) {
		return getFile(imageUri).delete();
	}

	@Override
	public void close() {
		// Nothing to do
	}

	/**
	 * 进行清除缓存文件
	 */
	@Override
	public void clear() {
		File[] files = cacheDir.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
	}

	/**
	 * Returns file object (not null) for incoming image URI. File object can reference to non-existing file.
	 * 创建缓存文件
	 */
	protected File getFile(String imageUri) {
		//生成指定格式的文件名
		String fileName = fileNameGenerator.generate(imageUri);
		File dir = cacheDir;
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			if (reserveCacheDir != null && (reserveCacheDir.exists() || reserveCacheDir.mkdirs())) {
				dir = reserveCacheDir;
			}
		}
		return new File(dir, fileName);
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setCompressFormat(Bitmap.CompressFormat compressFormat) {
		this.compressFormat = compressFormat;
	}

	public void setCompressQuality(int compressQuality) {
		this.compressQuality = compressQuality;
	}
}