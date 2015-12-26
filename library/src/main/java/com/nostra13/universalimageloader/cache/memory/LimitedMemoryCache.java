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
package com.nostra13.universalimageloader.cache.memory;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.utils.L;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实现BaseMemoryCache抽象类，扩展成有限制的内存缓存器,提供对象存储。所有存储的图片都不会超过限制。
 * 当前为抽象类，便于其他类进行相关扩展。
 * Limited cache. Provides object storing. Size of all stored bitmaps will not to exceed size limit (
 * {@link #getSizeLimit()}).<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see BaseMemoryCache
 * @since 1.0.0
 */
public abstract class LimitedMemoryCache extends BaseMemoryCache {
    /*正常最大缓存大小  默认MB单位*/
	private static final int MAX_NORMAL_CACHE_SIZE_IN_MB = 16;
	/*正常最大缓存大小 默认为16MB*/
	private static final int MAX_NORMAL_CACHE_SIZE = MAX_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024;
    /*大小限制*/
	private final int sizeLimit;

	private final AtomicInteger cacheSize;

	/**
	 * [注]LinkedList  硬缓存
	 * 所有存储对象引用集合。每一个对象都添加在集合的末尾。如果缓存的大小将要操作限制，那么集合的第一个对象将会删除并且被GC进行回收
	 * Contains strong references to stored objects. Each next object is added last. If hard cache size will exceed
	 * limit then first object is deleted (but it continue exist at {@link #softMap} and can be collected by GC at any
	 * time)
	 */
	private final List<Bitmap> hardCache = Collections.synchronizedList(new LinkedList<Bitmap>());

	/**
	 * @param sizeLimit Maximum size for cache (in bytes)
	 * 构造器 传入缓存限制的大小
	 */
	public LimitedMemoryCache(int sizeLimit) {
		this.sizeLimit = sizeLimit;
		cacheSize = new AtomicInteger();
		if (sizeLimit > MAX_NORMAL_CACHE_SIZE) {
			L.w("You set too large memory cache size (more than %1$d Mb)", MAX_NORMAL_CACHE_SIZE_IN_MB);
		}
	}

	/**
	 * 图片进行存入缓存中
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public boolean put(String key, Bitmap value) {
		boolean putSuccessfully = false;
		// Try to add value to hard cache
		// 获取图片大小
		int valueSize = getSize(value);
		int sizeLimit = getSizeLimit();
		int curCacheSize = cacheSize.get();
		if (valueSize < sizeLimit) {
			while (curCacheSize + valueSize > sizeLimit) {
				Bitmap removedValue = removeNext();
				if (hardCache.remove(removedValue)) {
					curCacheSize = cacheSize.addAndGet(-getSize(removedValue));
				}
			}
			hardCache.add(value);
			cacheSize.addAndGet(valueSize);

			putSuccessfully = true;
		}
		// Add value to soft cache    软缓存
		super.put(key, value);
		return putSuccessfully;
	}

	/**
	 * 从缓存中删除图片
	 * @param key
	 * @return
	 */
	@Override
	public Bitmap remove(String key) {
		Bitmap value = super.get(key);
		if (value != null) {
			if (hardCache.remove(value)) {
				cacheSize.addAndGet(-getSize(value));
			}
		}
		return super.remove(key);
	}

	@Override
	public void clear() {
		hardCache.clear();
		cacheSize.set(0);
		super.clear();
	}

	/**
	 * 获取缓存大小限制
	 * @return
	 */
	protected int getSizeLimit() {
		return sizeLimit;
	}

	protected abstract int getSize(Bitmap value);

	protected abstract Bitmap removeNext();
}
