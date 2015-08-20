package mike.myfaceidentification;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class CannyEdgDetect {
	/**
	 * 图片高度
	 */
	private static int height;

	/**
	 * 图片宽度
	 */
	private static int width;

	/**
	 * Canny边缘检测
	 * 
	 * 函数 Canny 采用 CANNY 算法发现输入图像的边缘而且在输出图像中标识这些边缘。 threshold1和threshold2
	 * 当中的小阈值用来控制边缘连接，大的阈值用来控制强边缘的初始分割。
	 * 
	 * Canny边缘检测算法步骤： (step1:用高斯滤波器平滑图象；) step2:计算梯度的幅值和方向； step3:对梯度幅值进行非极大值抑制；
	 * step4:用双阈值算法检测和连接边缘
	 * 
	 * @param sourceImage
	 *            输入图像
	 * @param lowThreshold
	 *            第一个阈值（低）
	 * @param highThreshold
	 *            第二个阈值（高）
	 * @return 用 Canny 边缘检测算法得到的边缘图
	 */
	public static Bitmap canny(Bitmap sourceImage, int lowThreshold,
			int highThreshold) {
		height = sourceImage.getHeight();
		width = sourceImage.getWidth();
		int picsize = width * height;

		// 梯度幅值表
		int[] gradeMagnitude = new int[picsize];
		// 梯度方向表
		int[] gradeOrientation = new int[picsize];

		// 计算方向导数和梯度的幅度
		grade(sourceImage, gradeMagnitude, gradeOrientation);

		// 应用非最大抑制,细化
		int[] edgeImage = NonmMaxSuppress(gradeMagnitude, gradeOrientation);
		// Normalize(sourceImage);

		// 边界提取与轮廓跟踪
		return thresholdingTracker(edgeImage, gradeMagnitude, lowThreshold,
				highThreshold);
	}

	/**
	 * 计算方向导数和梯度的幅度
	 * 
	 * @param grayImage
	 *            要计算的图象
	 * @param gradeMagnitude
	 *            要在其中存储结果的梯度数组
	 * @param gradeOrientation
	 *            要在其中存储结果的方向导数组
	 */
	private static void grade(Bitmap grayImage, int[] gradeMagnitude,
			int[] gradeOrientation) {
		int height = grayImage.getHeight();
		int width = grayImage.getWidth();
		int[] srcGray = new int[width * height];
		// 提取灰度值
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				srcGray[i + j * width] = 0xff & grayImage.getPixel(i, j);

		for (int i = 1; i < width - 1; i++) {
			for (int j = 1; j < height - 1; j++) {
				int x = srcGray[(i + 1) + j * width]
						- srcGray[(i - 1) + j * width];// X方向导数
				int y = srcGray[i + (j + 1) * width]
						- srcGray[i + (j - 1) * width];// Y方向导数

				gradeMagnitude[i + j * width] = (int) Math.sqrt(x * x + y * y);
				// 方向导数在非最大抑制时使用，使用时，只需知道 X 方向导数和 Y 方向导数的同号或异号情况
				gradeOrientation[i + j * width] = x * y;
			}
		}
	}

	/**
	 * 非最大抑制
	 * 
	 * @param gradeMagnitude
	 *            梯度数组
	 * @param gradeOrientation
	 *            方向数组
	 * @return 非最大抑制后的边缘数组
	 */
	private static int[] NonmMaxSuppress(int[] gradeMagnitude,
			int[] gradeOrientation) {
		int[] edgeImage = new int[width * height];

		for (int i = 1; i < width - 1; i++) {
			for (int j = 1; j < height - 1; j++) {
				if (gradeMagnitude[i + width * j] == 0) {
					edgeImage[i + width * j] = 0;
					continue;
				}
				int n1 = 0, n2 = 0;

				// 判断 X 方向导数和 Y 方向导数的同号或异号情况
				if (gradeOrientation[i + width * j] > 0) {
					n1 = gradeMagnitude[i - 1 + width * (j - 1)];
					n2 = gradeMagnitude[i + 1 + width * (j + 1)];
				} else {
					n1 = gradeMagnitude[i + 1 + width * (j - 1)];
					n2 = gradeMagnitude[i - 1 + width * (j + 1)];
				}
				// 当前象素的梯度是局部的最大值
				// 该点可能是个边界点
				if (gradeMagnitude[i + width * j] >= n1
						&& gradeMagnitude[i + width * j] >= n2) {
					edgeImage[i + width * j] = 128;
				}
				// 不可能是边界点
				else {
					edgeImage[i + width * j] = 0;
				}
			}
		}
		return edgeImage;
	}

	/**
	 * 边界提取与轮廓跟踪,利用函数寻找边界起点
	 * 
	 * @param edgeImage
	 *            边缘数组
	 * @param gradeMagnitude
	 *            梯度数组
	 * @param lowThreshold
	 *            第一个阈值（低）
	 * @param highThreshold
	 *            第二个阈值（高）
	 * @return 用 Canny 边缘检测算法得到的边缘图
	 */
	private static Bitmap thresholdingTracker(int[] edgeImage,
			int[] gradeMagnitude, int lowThreshold, int highThreshold) {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				// 如果该象素是可能的边界点，并且梯度大于高阈值，该象素作为
				// 一个边界的起点
				if (edgeImage[i + width * j] == 128
						&& gradeMagnitude[i + width * j] >= highThreshold) {
					// 把该点设置成为边界点
					edgeImage[i + width * j] = 255;
					// 以该点为中心进行跟踪
					follow(edgeImage, i, j, gradeMagnitude, lowThreshold);
				}
			}
		}

		Bitmap destImage = Bitmap.createBitmap(width, height, Config.RGB_565);
		// BufferedImage destImage = new BufferedImage(width, height,
		// BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++)
				if (edgeImage[i + width * j] == 255)
					destImage.setPixel(i, j, -1);
				else
					destImage.setPixel(i, j, 0xff000000);
		}
		return destImage;
	}

	/**
	 * 在8邻近区域里搜索
	 * 
	 * @param edgeImage
	 *            边缘数组
	 * @param x
	 *            图像元素的x坐标位置
	 * @param y
	 *            图像元素的y坐标位置
	 * @param gradeMagnitude
	 *            梯度数组
	 * @param lowThreshold
	 *            第一个阈值（低）
	 */
	// 暂时用限制递归次数处理
	private static int mike = 0; // 我设置的控制递归次数
	private static int ReNum = 150;// 递归次数

	private static void follow(int[] edgeImage, int x, int y,
			int[] gradeMagnitude, int lowThreshold) {
		mike++;
		// 对8邻域象素进行查询
		int[][] dir = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 },
				{ 1, -1 }, { 1, 0 }, { 1, 1 } };
		for (int k = 0; k < 8; k++) {
			int ii = x + dir[k][0];
			int jj = y + dir[k][1];
			// 如果该象素为可能的边界点，又没有处理过
			// 并且梯度大于阈值
			if (edgeImage[ii + width * jj] == 128
					&& gradeMagnitude[ii + width * jj] >= lowThreshold) {
				// 把该点设置成为边界点
				edgeImage[ii + width * jj] = 255;
				if (mike <= ReNum) {
					// System.out.println(mike);
					// 以该点为中心进行跟踪
					follow(edgeImage, ii, jj, gradeMagnitude, lowThreshold);
				}
			}
		}
	}

	// 计算灰度投影曲线
	public static void Normalize(Bitmap grayImage) {
		int height = grayImage.getHeight();
		int width = grayImage.getWidth();
		int[] srcGray = new int[width * height];
		int[] shadowX = new int[width];
		int[] shadowY = new int[height];
		int sum = 0;
		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				srcGray[i + j * width] = 0xff & grayImage.getPixel(i, j);
		// 计算x轴灰度投影函数
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				sum = srcGray[i + j * width] + sum;
			}
			shadowX[i] = (int) (sum / height);
			sum = 0;
			System.out.println(shadowX[i] + "  " + i);
		}
		System.out.println("/****************************/");
		// 计算y轴灰度函数投影
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				sum = srcGray[i + j * height] + sum;
			}
			shadowY[i] = (int) (sum / width);
			sum = 0;
			System.out.println(shadowY[i] + "  " + i);
		}
	}
}
