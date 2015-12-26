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
package com.nostra13.universalimageloader.cache.memory.impl;

import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.memory.LimitedMemoryCache;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 实现LimitedMemoryCache抽象类，该为有限缓存，所有存储的图片确保不会超过限制大小。当缓存大小已经达到极限的时候，
 * 缓存器会根据w文件FIFO算法进行清理相关缓存文件。
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then cache clearing is processed by FIFO principle.<br />
 * <br />
 * 该缓存器使用强引用和弱引用来进行存储图片。
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public class FIFOLimitedMemoryCache extends LimitedMemoryCache {
	/**
	 * 开辟硬缓存集合  采用LinkedList
	 */
	private final List<Bitmap> queue = Collections.synchronizedList(new LinkedList<Bitmap>());

	/**
	 * FIFO限制缓存器构造器
	 * @param sizeLimit
	 */
	public FIFOLimitedMemoryCache(int sizeLimit) {
		super(sizeLimit);
	}

	/**
	 * 图片对象添加到缓存中
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public boolean put(String key, Bitmap value) {
		if (super.put(key, value)) {
			queue.add(value);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 根据key从缓存中删除文件
	 * @param key
	 * @return
	 */
	@Override
	public Bitmap remove(String key) {
		Bitmap value = super.get(key);
		if (value != null) {
			queue.remove(value);
		}
		return super.remove(key);
	}

	/**
	 * 清空缓存
	 */
	@Override
	public void clear() {
		queue.clear();
		super.clear();
	}

	/**
	 * 计算图片大小
	 * @param value
	 * @return
	 */
	@Override
	protected int getSize(Bitmap value) {
		return value.getRowBytes() * value.getHeight();
	}

	/**
	 * 进行移动
	 * @return
	 */
	@Override
	protected Bitmap removeNext() {
		return queue.remove(0);
	}

	@Override
	protected Reference<Bitmap> createReference(Bitmap value) {
		return new WeakReference<Bitmap>(value);
	}
}
