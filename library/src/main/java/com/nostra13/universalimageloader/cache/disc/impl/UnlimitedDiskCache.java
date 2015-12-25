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

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;

import java.io.File;

/**
 * 默认无限制本地文件系统缓存(磁盘缓存)
 * Default implementation of {@linkplain com.nostra13.universalimageloader.cache.disc.DiskCache disk cache}.
 * Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public class UnlimitedDiskCache extends BaseDiskCache {
	/** @param cacheDir Directory for file caching */
	public UnlimitedDiskCache(File cacheDir) {
		super(cacheDir);
	}

	/**
	 * 构造器   无限制磁盘缓存器初始化
	 * @param cacheDir         Directory for file caching    缓存文件夹
	 * @param reserveCacheDir  备用缓存文件夹
	 *                         null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 */
	public UnlimitedDiskCache(File cacheDir, File reserveCacheDir) {
		super(cacheDir, reserveCacheDir);
	}

	/**
	 * 构造器   无限制磁盘缓存器初始化
	 * @param cacheDir          Directory for file caching 缓存文件夹
	 * @param reserveCacheDir    备用缓存文件夹
	 * null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
	 * @param fileNameGenerator    缓存文件名生成器
	 * {@linkplain com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator
	 *                          Name generator} for cached files
	 */
	public UnlimitedDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator) {
		super(cacheDir, reserveCacheDir, fileNameGenerator);
	}
}
