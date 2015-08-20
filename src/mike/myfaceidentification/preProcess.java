package mike.myfaceidentification;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;

public class preProcess {

	// 灰度变换范围(这里灰度变换后的图像灰度值暂扩展为0-255，灰度变换前的图像范围暂固定)
	// 数值固定始终无法适应各种图片，最好能够根据图像亮度(或对比度?)来进行数值变换
	static int fM = 200;
	static int fm = 50;
	static int gN = 255;
	static int gn = 0;
	// 对比度参数
	static double Contrast = 1.125;

	// 把位图转换为灰度图像(www.blogjava.net/jayslong/archive/2011/03/23/346860.html)
	public static Bitmap toGrayscale(Bitmap bmpOriginal) {
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();
		/**
		 * To draw something, you need 4 basic components: A Bitmap to hold the
		 * pixels, a Canvas to host the draw calls (writing into the bitmap), a
		 * drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint (to
		 * describe the colors and styles for the drawing)
		 */
		// 创建一个指定宽度，高度的图像 RGB_565
		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
				Bitmap.Config.RGB_565);
		// 在bmpGrayscale上创建一个Canvas对象(类似"画布")
		Canvas c = new Canvas(bmpGrayscale);
		// 创建一个Paint对象(类似"涂料")
		Paint paint = new Paint();
		// 创建一个ColorMatrix对象(5x4 matrix for transforming the color+alpha
		// components of a Bitmap)
		ColorMatrix cm = new ColorMatrix();
		// 设置饱和度(范围0~1 0为灰度)
		cm.setSaturation(0);
		// Create a colorfilter that transforms colors through a 4x5 color
		// matrix
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		// Draw the specified bitmap, with its top/left corner at (x,y), using
		// the specified paint, transformed by the current matrix
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	// 增加对比度
	public static Bitmap incrContrast(Bitmap bm) {
		// 初始化
		int height = bm.getHeight();
		int width = bm.getWidth();
		// 像素数组
		int[] pixels = new int[width * height];
		// 转换为灰度图像
		bm = preProcess.toGrayscale(bm);
		// 像素点颜色值
		int grey;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				grey = bm.getPixel(i, j);
				grey = 0xff & bm.getPixel(i, j);
				grey = (int) (Contrast * grey);
				grey = grey > 255 ? 255 : grey;
				pixels[i + j * width] = Color.rgb(grey, grey, grey);
			}
		}
		return Bitmap.createBitmap(pixels, width, height, Config.RGB_565);
	}

	// 简单线性灰度变换
	public static Bitmap linearGray(Bitmap bm) {
		// 初始化
		int height = bm.getHeight();
		int width = bm.getWidth();
		// 像素数组
		int[] pixels = new int[width * height];
		// 转换为灰度图像
		bm = preProcess.toGrayscale(bm);
		// 像素点颜色值
		int grey;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				grey = bm.getPixel(i, j);
				grey = 0xff & bm.getPixel(i, j);
				// 灰度变换
				if (grey <= fm)
					grey = 0;
				else if (grey > fM)
					grey = 255;
				else
					grey = (gN - gn) * (grey - fm) / (fM - fm) + gn;
				if (grey < 0)
					grey = 0;
				else if (grey > 255)
					grey = 255;
				pixels[i + j * width] = Color.rgb(grey, grey, grey);
			}
		}
		return Bitmap.createBitmap(pixels, width, height, Config.RGB_565);
	}

	// 中值滤波(图像边缘像素不考虑，取含9个像素的滑动窗口，方法：排序找中值)
	public static int med(int pixel, int pixels[], int width, int num) {
		// 滑动窗口
		int sortpixel[] = new int[9];
		// 抽取数值
		for (int i = -1; i < 2; i++) {
			sortpixel[i + 1] = pixels[num - (width + i)];
			sortpixel[i + 4] = pixels[num + i];
			sortpixel[i + 7] = pixels[num + (width + i)];
		}
		// 排序
		int item, j, k;
		for (j = 1; j < 9; j++) {
			item = sortpixel[j];
			k = j - 1;
			while (item < sortpixel[k]) {
				sortpixel[k + 1] = sortpixel[k];
				k--;
				if (k < 0)
					break;
			}
			sortpixel[k + 1] = item;
		}
		return sortpixel[4];
	}

	// 使用中值滤波(已经修改过的元素会影响后面滑动窗口元素的选取)
	public static Bitmap medfilter(Bitmap bm) {
		int height = bm.getHeight();
		int width = bm.getWidth();
		int[] pixels = new int[width * height];
		// 得到图像数据的每个像素点的灰度值
		bm.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int i = 1; i < height - 1; i++) {
			for (int j = 1; j < width - 1; j++) {
				pixels[i * width + j] = preProcess.med(pixels[i * width + j],
						pixels, width, i * width + j);
			}
		}
		return Bitmap.createBitmap(pixels, width, height, Config.RGB_565);
	}

}
