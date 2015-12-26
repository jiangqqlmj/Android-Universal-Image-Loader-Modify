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
package com.nostra13.universalimageloader.core.assist;

/**
 * 提供Image尺寸信息封装
 * Present width and height values
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public class ImageSize {

	private static final int TO_STRING_MAX_LENGHT = 9; // "9999x9999".length()
	private static final String SEPARATOR = "x";

	private final int width;
	private final int height;

	/**
	 * 构造方法
	 * @param width    宽度
	 * @param height   高度
	 */
	public ImageSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * 构造方法      -传入旋转角度
	 * @param width
	 * @param height
	 * @param rotation
	 */
	public ImageSize(int width, int height, int rotation) {
		if (rotation % 180 == 0) {
			this.width = width;
			this.height = height;
		} else {
			this.width = height;
			this.height = width;
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Scales down dimensions in <b>sampleSize</b> times. Returns new object.
	 * 进行尺寸缩小
	 */
	public ImageSize scaleDown(int sampleSize) {
		return new ImageSize(width / sampleSize, height / sampleSize);
	}

	/**
	 * Scales dimensions according to incoming scale. Returns new object.
	 * 进行尺寸放大
	 */
	public ImageSize scale(float scale) {
		return new ImageSize((int) (width * scale), (int) (height * scale));
	}

	@Override
	public String toString() {
		return new StringBuilder(TO_STRING_MAX_LENGHT).append(width).append(SEPARATOR).append(height).toString();
	}
}
