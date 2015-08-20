package mike.myfaceidentification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

public class Feature {

	// 方法参数
	public static final int STOREAGE = 1;
	public static final int MATCH = 0;

	// 匹配阈值
	private static final double FIRST_MATCH = 0.997;
	private static final double SECOND_MATCH = 0.995;

	// 延伸边界
	public static int Left;
	public static int Right;
	public static int Top;
	public static int Bottom;

	// 微调误差[像素]
	private static final int D = 1;
	// 保存中间过程的位图对象
	public static Bitmap bmp;

	// 特征提取(名字应该改成特征边缘),paras表示一种查找范围,div为用于减小paras
	public static void featurePickUp(PointF pf, Bitmap bm, int paras, int div) {

		// 复位
		Left = Right = Top = Bottom = -1;

		int width = bm.getWidth();
		int height = bm.getHeight();
		int[] Imgpixels = new int[width * height];
		// 提取像素
		bm.getPixels(Imgpixels, 0, width, 0, 0, width, height);

		// 对8邻域象素进行查询
		int[][] dir = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 },
				{ 1, -1 }, { 1, 0 }, { 1, 1 } };
		// 使用队列
		LinkedList<PointF> ll = new LinkedList<PointF>();
		ll.add(pf);
		// 起始点
		Left = Right = (int) pf.x;
		Top = Bottom = (int) pf.y;
		Imgpixels[(int) pf.x + (int) pf.y * width] = Color.BLACK;

		for (int i = 1; i < paras; i++) {
			int xx = (int) pf.x;
			int yy = (int) pf.y;
			// 上下伸展取参数的一半
			if (i <= (paras / div)) {
				// 这是一个轮廓点
				if ((0xff & Imgpixels[xx + (yy - i) * width]) == 255) {
					// 构造结点并加入队列
					PointF elet = new PointF();
					elet.x = xx;
					elet.y = yy - i;
					ll.add(elet);
					// 上移
					Top = yy - i;
					// 排除掉这个点
					Imgpixels[xx + (yy - i) * width] = Color.RED;
				}
			}
			if (i <= (paras / div)) {
				// 这是一个轮廓点
				if ((0xff & Imgpixels[xx + (yy + i) * width]) == 255) {
					// 构造结点并加入队列
					PointF eleb = new PointF();
					eleb.x = xx;
					eleb.y = yy + i;
					ll.add(eleb);
					// 下移
					Bottom = yy + i;
					// 排除掉这个点
					Imgpixels[xx + (yy + i) * width] = Color.RED;
				}
			}
			// 这是一个轮廓点
			if ((0xff & Imgpixels[(xx - i) + yy * width]) == 255) {
				// 构造结点并加入队列
				PointF elel = new PointF();
				elel.x = xx - i;
				elel.y = yy;
				ll.add(elel);
				// 左移
				Left = xx - i;
				// 排除掉这个点
				Imgpixels[(xx - i) + yy * width] = Color.RED;
			}
			// 这是一个轮廓点
			if ((0xff & Imgpixels[(xx + i) + yy * width]) == 255) {
				// 构造结点并加入队列
				PointF eler = new PointF();
				eler.x = xx + i;
				eler.y = yy;
				ll.add(eler);
				// 右移
				Right = xx + i;
				// 排除掉这个点
				Imgpixels[(xx + i) + yy * width] = Color.RED;
			}
		}
		// 8邻域搜索
		while (!ll.isEmpty()) {
			PointF q = ll.removeFirst();
			for (int k = 0; k < 8; k++) {
				int px = (int) q.x + dir[k][0];
				int py = (int) q.y + dir[k][1];
				// 这是一个轮廓点
				if ((0xff & Imgpixels[px + py * width]) == 255) {
					// 构造结点并加入队列
					PointF ele = new PointF();
					ele.x = px;
					ele.y = py;
					ll.add(ele);
					// 排除掉这个点
					Imgpixels[px + py * width] = Color.RED;
					// 边界比较
					if (px > Right)
						Right = px;
					else if (px < Left)
						Left = px;
					if (py > Bottom)
						Bottom = py;
					else if (py < Top)
						Top = py;
				}
			}
		}
		bmp = Bitmap.createBitmap(Imgpixels, width, height, Config.RGB_565);
	}

	// 双眼各自的中心坐标定位
	public static void EyesLocate(PointF pointl, PointF pointr, float dist,
			PointF midpoint, int cut_x, int cut_y, float scalex, float scaley) {

		// 左眼坐标
		pointl.x = (midpoint.x - dist / 2 - cut_x) * scalex;
		pointl.y = (midpoint.y + D - cut_y) * scaley;
		// 右眼坐标
		pointr.x = (midpoint.x + dist / 2 - cut_x) * scalex;
		pointr.y = (midpoint.y + D - cut_y) * scaley;

		// 输出左眼和右眼坐标
		System.out.println("左眼：" + pointl.x + " " + pointl.y);
		System.out.println("右眼：" + pointr.x + " " + pointr.y);
	}

	public static boolean ConstructVector(Rect lEyeR, Rect rEyeR, Rect mr,
			Point pNose, int param) {
		// 特征向量F1
		int[] F1 = new int[] { (lEyeR.right + lEyeR.left) / 2,
				(lEyeR.bottom + lEyeR.top) / 2, (rEyeR.right + rEyeR.left) / 2,
				(rEyeR.bottom + rEyeR.top) / 2, (mr.right + mr.left) / 2,
				(mr.bottom + mr.top) / 2, pNose.x, pNose.y };
		// 特征向量F2
		int[] F2 = new int[] { lEyeR.right - lEyeR.left,
				lEyeR.bottom - lEyeR.top, rEyeR.right - rEyeR.left,
				rEyeR.bottom - rEyeR.top, pNose.y - pNose.x,
				mr.right - mr.left, mr.bottom - mr.top };
		if (param == STOREAGE) {
			StorageToFile(F1, F2);
			return true;
		} else if (param == MATCH)
			return FaceMatch(F1, F2);
		return true;
	}

	// 存储到文件
	public static void StorageToFile(int[] f1, int[] f2) {
		// 转换成字符串,使用JSONArray
		JSONArray f1array = new JSONArray();
		for (int i = 0; i < f1.length; i++) {
			f1array.put(f1[i]);
		}
		String f1string = f1array.toString();
		JSONArray f2array = new JSONArray();
		for (int i = 0; i < f2.length; i++) {
			f2array.put(f2[i]);
		}
		String f2string = f2array.toString();
		File file = new File("./sdcard/mikedata.mik");
		OutputStreamWriter osw = null;
		try {
			if (!file.exists())
				file.createNewFile();
			osw = new OutputStreamWriter(new FileOutputStream(file));
			osw.write(f1string + "\n");
			osw.write(f2string + "\n");
			osw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean FaceMatch(int[] f1, int[] f2) {
		JSONArray f1j, f2j;
		int[] of1 = new int[8];
		int[] of2 = new int[7];
		// 首先从文件中取出数据
		File file = new File("./sdcard/mikedata.mik");
		InputStreamReader isr = null;
		if (!file.exists()) {
			System.out.println("数据文件不存在");
			return false;
		}
		try {
			isr = new InputStreamReader(new FileInputStream(file));
			// 建立读缓冲区
			BufferedReader br = new BufferedReader(isr);
			String str1 = br.readLine();
			f1j = new JSONArray(str1);
			for (int i = 0; i < f1j.length(); i++) {
				of1[i] = f1j.getInt(i);
			}
			String str2 = br.readLine();
			f2j = new JSONArray(str2);
			for (int i = 0; i < f2j.length(); i++) {
				of2[i] = f2j.getInt(i);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 匹配过程,采用欧氏距离,二次匹配
		// 第一次
		// int sum = 0;
		// for(int i=0;i<f1.length;i++){
		// sum += (f1[i]-of1[i])*(f1[i]-of1[i]);
		// }
		// double score1 = Math.sqrt(sum);
		// System.out.println("第一次："+score1);
		// // 第二次
		// sum = 0;
		// for(int i=0;i<f2.length;i++){
		// sum += (f2[i]-of2[i])*(f2[i]-of2[i]);
		// }
		// double score2 = Math.sqrt(sum);
		// System.out.println("第二次："+score2);

		// 使用余弦相似度进行匹配,二次
		double score = -1;
		score = CosSimilarity(of1, f1);
		System.out.println("第一次：" + score);
		if (score < FIRST_MATCH)
			return false;
		score = CosSimilarity(of2, f2);
		System.out.println("第二次：" + score);
		if (score < SECOND_MATCH)
			return false;
		return true;

		// 余弦相似度,一次
		// int[] v1 = new int[] { of1[0], of1[1], of1[2], of1[3], of1[4],
		// of1[5],
		// of1[6], of1[7], of2[0], of2[1], of2[2], of2[3], of2[4], of2[5],
		// of2[6] };
		// int[] v2 = new int[] { f1[0], f1[1], f1[2], f1[3], f1[4], f1[5],
		// f1[6],
		// f1[7], f2[0], f2[1], f2[2], f2[3], f2[4], f2[5], f2[6] };
		// score = CosSimilarity(v1, v2);
		// System.out.println("结果："+score);

		// if (score1 <= 15 && score2 <= 15)
		// StorageToFile(f1, f2);
	}

	private static double CosSimilarity(int[] vec1, int[] vec2) {
		double similarity = 0.0, numerator = 0.0, denominator1 = 0.0, denominator2 = 0.0;
		for (int i = 0; i < vec1.length; i++) {
			numerator += vec1[i] * vec2[i];
			denominator1 += vec1[i] * vec1[i];
			denominator2 += vec2[i] * vec2[i];
		}
		similarity = numerator / (Math.sqrt(denominator1 * denominator2));
		return similarity;
	}

}
