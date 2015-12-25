/*******************************************************************************
 * Copyright 2014 Sergey Tarasevich
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
package com.nostra13.universalimageloader.cache.disc.impl.ext;

import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.utils.IoUtils;
import com.nostra13.universalimageloader.utils.L;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 基于LRU算法文件缓存  实现DiskCache接口
 * Disk cache based on "Least-Recently Used" principle. Adapter pattern, adapts
 * {@link com.nostra13.universalimageloader.cache.disc.impl.ext.DiskLruCache DiskLruCache} to
 * {@link com.nostra13.universalimageloader.cache.disc.DiskCache DiskCache}
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see FileNameGenerator
 * @since 1.9.2
 */
public class LruDiskCache implements DiskCache {
	/**
	 * {@value}
	 * 默认缓冲区大小
	 */
	public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32 Kb
	/**
	 * {@value}
	 * 图片压缩格式
	 * */
	public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
	/** {@value}
	 * 默认图片压缩质量
	 */
	public static final int DEFAULT_COMPRESS_QUALITY = 100;

	private static final String ERROR_ARG_NULL = " argument must be not null";
	private static final String ERROR_ARG_NEGATIVE = " argument must be positive number";
    /*本地文件系统(磁盘)LRU缓存器*/
	protected DiskLruCache cache;
	/*备用缓存文件*/
	private File reserveCacheDir;
    /*缓存文件名 命名生成器*/
	protected final FileNameGenerator fileNameGenerator;
    /*缓冲区  这边已经进行复制初始化*/
	protected int bufferSize = DEFAULT_BUFFER_SIZE;
    /*图片压缩质量  这边赋值默认数据*/
	protected Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
	protected int compressQuality = DEFAULT_COMPRESS_QUALITY;

	/**
	 * LruDiskCache 图片本地文件缓存器初始化
	 * @param cacheDir          Directory for file caching
	 * @param fileNameGenerator {@linkplain com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
	 *                          Name generator} for cached files. Generated names must match the regex
	 *                          <strong>[a-z0-9_-]{1,64}</strong>
	 * @param cacheMaxSize      Max cache size in bytes. <b>0</b> means cache size is unlimited.
	 * @throws IOException if cache can't be initialized (e.g. "No space left on device")
	 */
	/**
	 * LruDiskCache 图片本地文件缓存器初始化
	 * @param cacheDir            缓存器文件
	 * @param fileNameGenerator   缓存文件名命名规则生成器
	 * @param cacheMaxSize        最大缓存大小
	 * @throws IOException
	 */
	public LruDiskCache(File cacheDir, FileNameGenerator fileNameGenerator, long cacheMaxSize) throws IOException {
		this(cacheDir, null, fileNameGenerator, cacheMaxSize, 0);
	}

	/**
	 * LruDiskCache 图片本地文件缓存器初始化
	 * @param cacheDir          Directory for file caching
	 * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 * @param fileNameGenerator {@linkplain com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
	 *                          Name generator} for cached files. Generated names must match the regex
	 *                          <strong>[a-z0-9_-]{1,64}</strong>
	 * @param cacheMaxSize      Max cache size in bytes. <b>0</b> means cache size is unlimited.
	 * @param cacheMaxFileCount Max file count in cache. <b>0</b> means file count is unlimited.
	 * @throws IOException if cache can't be initialized (e.g. "No space left on device")
	 */
	public LruDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator, long cacheMaxSize,
			int cacheMaxFileCount) throws IOException {
		if (cacheDir == null) {
			throw new IllegalArgumentException("cacheDir" + ERROR_ARG_NULL);
		}
		if (cacheMaxSize < 0) {
			throw new IllegalArgumentException("cacheMaxSize" + ERROR_ARG_NEGATIVE);
		}
		if (cacheMaxFileCount < 0) {
			throw new IllegalArgumentException("cacheMaxFileCount" + ERROR_ARG_NEGATIVE);
		}
		if (fileNameGenerator == null) {
			throw new IllegalArgumentException("fileNameGenerator" + ERROR_ARG_NULL);
		}
        //如果传入最大缓存大小为0 ,那么设置Long的最大值
		if (cacheMaxSize == 0) {
			cacheMaxSize = Long.MAX_VALUE;
		}
		//如果传入最大缓存文件数量为0 ，那么设置Integer的最大值
		if (cacheMaxFileCount == 0) {
			cacheMaxFileCount = Integer.MAX_VALUE;
		}

		this.reserveCacheDir = reserveCacheDir;
		this.fileNameGenerator = fileNameGenerator;
		//初始化缓存
		initCache(cacheDir, reserveCacheDir, cacheMaxSize, cacheMaxFileCount);
	}

	/**
	 * 初始化本地文件 图片缓存器
	 * @param cacheDir          图片缓存路径
	 * @param reserveCacheDir     备用图片缓存路径
	 * @param cacheMaxSize        最大缓存大小
	 * @param cacheMaxFileCount    最大缓存图片数量
	 * @throws IOException
	 */
	private void initCache(File cacheDir, File reserveCacheDir, long cacheMaxSize, int cacheMaxFileCount)
			throws IOException {
		try {
			cache = DiskLruCache.open(cacheDir, 1, 1, cacheMaxSize, cacheMaxFileCount);
		} catch (IOException e) {
			L.e(e);
			if (reserveCacheDir != null) {
				initCache(reserveCacheDir, null, cacheMaxSize, cacheMaxFileCount);
			}
			if (cache == null) {
				throw e; //new RuntimeException("Can't initialize disk cache", e);
			}
		}
	}

	/**
	 * 获取LRU文件缓存文件路径
	 * @return
	 */
	@Override
	public File getDirectory() {
		return cache.getDirectory();
	}

	/**
	 * 根据的URL连接 从LRU文件器重获取文件
	 * @param imageUri Original image URI
	 * @return
	 */
	@Override
	public File get(String imageUri) {
		DiskLruCache.Snapshot snapshot = null;
		try {
			snapshot = cache.get(getKey(imageUri));
			return snapshot == null ? null : snapshot.getFile(0);
		} catch (IOException e) {
			L.e(e);
			return null;
		} finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}
	}

	/**
	 * 把图片流数据保存到缓存中
	 * @param imageUri    Original image URI  图像URL连接地址
	 * @param imageStream Input stream of image (shouldn't be closed in this method)  图片流
	 * @param listener    图片流拷贝监听器
	 * Listener for saving progress, can be ignored if you don't use
	 *                    {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
	 *                    progress listener} in ImageLoader calls
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
		DiskLruCache.Editor editor = cache.edit(getKey(imageUri));
		if (editor == null) {
			return false;
		}
        //流写入文件
		OutputStream os = new BufferedOutputStream(editor.newOutputStream(0), bufferSize);
		boolean copied = false;
		try {
			copied = IoUtils.copyStream(imageStream, os, listener, bufferSize);
		} finally {
			IoUtils.closeSilently(os);
			if (copied) {
				editor.commit();
			} else {
				editor.abort();
			}
		}
		return copied;
	}

	/**
	 * 把图片bitmap存入到缓存器中
	 * @param imageUri Original image URI  图片URL地址
	 * @param bitmap   Image bitmap   需要保存的图片
	 * @return
	 * @throws IOException
	 */
	@Override
	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
		DiskLruCache.Editor editor = cache.edit(getKey(imageUri));
		if (editor == null) {
			return false;
		}
        //流写入文件
		OutputStream os = new BufferedOutputStream(editor.newOutputStream(0), bufferSize);
		boolean savedSuccessfully = false;
		try {
			savedSuccessfully = bitmap.compress(compressFormat, compressQuality, os);
		} finally {
			IoUtils.closeSilently(os);
		}
		if (savedSuccessfully) {
			editor.commit();
		} else {
			editor.abort();
		}
		return savedSuccessfully;
	}

	/**
	 * 根据图片URL地址从缓存中删除文件
	 * @param imageUri Image URI
	 * @return
	 */
	@Override
	public boolean remove(String imageUri) {
		try {
			return cache.remove(getKey(imageUri));
		} catch (IOException e) {
			L.e(e);
			return false;
		}
	}

	/**
	 * 进行关闭缓存
	 */
	@Override
	public void close() {
		try {
			cache.close();
		} catch (IOException e) {
			L.e(e);
		}
		cache = null;
	}

	/**
	 * 进行清除缓存数据
	 */
	@Override
	public void clear() {
		try {
			cache.delete();
		} catch (IOException e) {
			L.e(e);
		}
		try {
			//这边重新初始化缓存器   缓存器中文件为空
			initCache(cache.getDirectory(), reserveCacheDir, cache.getMaxSize(), cache.getMaxFileCount());
		} catch (IOException e) {
			L.e(e);
		}
	}

	/**
	 * 根据图片URL地址进行生成缓存文件名
	 * @param imageUri
	 * @return
	 */
	private String getKey(String imageUri) {
		return fileNameGenerator.generate(imageUri);
	}

	/**
	 * 设置缓冲区大小
	 * @param bufferSize
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * 设置图片压缩格式
	 * @param compressFormat
	 */
	public void setCompressFormat(Bitmap.CompressFormat compressFormat) {
		this.compressFormat = compressFormat;
	}

	/**
	 * 设置图片压缩质量
	 * @param compressQuality
	 */
	public void setCompressQuality(int compressQuality) {
		this.compressQuality = compressQuality;
	}
}
