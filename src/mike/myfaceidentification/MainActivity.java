package mike.myfaceidentification;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int MENU_SAVE = 100;
	private static final int MENU_ABOUT = 101;
	// “进度”文本对象
	TextView textView = null;
	// 进度条对象
	ProgressBar bar = null;
	// 进程分段数
	private int N = 6;
	// 按钮对象
	Button startButton = null;
	Button cancelButton = null;
	Button exitButton = null;
	Button lookButton = null;
	// 图像控件对象
	ImageView myImageView = null;
	ImageView myImageView2 = null;
	ImageView myImageView3 = null;
	ImageView myImageView4 = null;
	// 位图对象
	Bitmap bm = null;
	// debug用位图
	Bitmap nbm = null;
	// 终止线程标志
	Boolean cancel = false;
	// 分析结束标志
	Boolean finishAnalyse = false;
	// 眼睛坐标
	PointF leftEye = new PointF();
	PointF rightEye = new PointF();
	// Handler对象,控制线程
	Handler updateBarHandler = new Handler();
	// 双眼距离值
	private float dist = -1;
	// 双眼中点
	PointF point = new PointF();
	// 伸展参数
	private int STRECH_EYE = 40;
	private int STRECH_MOUTH = 60;
	private int STRECH_NOSE = 30;
	// 偏移增量
	private int LAMDA_a = 5;
	private int LAMDA_b = 20;
	private int BETA_a = 0;
	private int BETA_b = 15;

	private Rect lEyeRect;
	private Rect rEyeRect;
	private Rect mRect;
	Point pt_nose;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 设置窗口为全屏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		// 控件绑定并监听
		textView = (TextView) findViewById(R.id.mytext);
		myImageView = (ImageView) findViewById(R.id.myimage);
		myImageView2 = (ImageView) findViewById(R.id.myimage2);
		myImageView3 = (ImageView) findViewById(R.id.myimage3);
		myImageView4 = (ImageView) findViewById(R.id.myimage4);
		bar = (ProgressBar) findViewById(R.id.myBar);
		startButton = (Button) findViewById(R.id.mybutton);
		startButton.setOnClickListener(new Button1Listener());
		cancelButton = (Button) findViewById(R.id.mybutton2);
		cancelButton.setOnClickListener(new Button2Listener());
		cancelButton.setVisibility(View.GONE);
		exitButton = (Button) findViewById(R.id.mybutton3);
		exitButton.setOnClickListener(new Button3Listener());
		lookButton = (Button) findViewById(R.id.mybutton4);
		lookButton.setOnClickListener(new Button4Listener());
		lookButton.setVisibility(View.GONE);
		textView.setVisibility(View.GONE);
	}

	// start按钮的监听器
	class Button1Listener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// 从设备中读图
			bm = myCamrActivity.bitmap;

			cancel = false;

			cancelButton.setVisibility(View.VISIBLE);
			exitButton.setVisibility(View.GONE);
			startButton.setVisibility(View.GONE);
			textView.setVisibility(View.VISIBLE);
			bar.setVisibility(View.VISIBLE);
			// 设置进度为0
			bar.setProgress(0);
			// 使用Handler对象
			updateBarHandler.post(FaceThread);
		}

	}

	// cancel按钮的监听器
	class Button2Listener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			cancel = true;
			cancelButton.setVisibility(View.GONE);
			exitButton.setVisibility(View.VISIBLE);
			startButton.setVisibility(View.VISIBLE);
			bar.setVisibility(View.GONE);
			textView.setVisibility(View.GONE);
		}

	}

	// exit按钮的监听器
	class Button3Listener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// 先让程序到Home界面，然后再将process杀死
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			// 根据程序在系统进程内的ID进行强制结束当前程序进程
			android.os.Process.killProcess(android.os.Process.myPid());
		}

	}

	// look按钮的监听器
	class Button4Listener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			textView.setVisibility(View.GONE);
			bar.setVisibility(View.GONE);
			lookButton.setVisibility(View.GONE);
			exitButton.setVisibility(View.GONE);

			myImageView.setVisibility(View.VISIBLE);
			myImageView2.setVisibility(View.VISIBLE);
			myImageView3.setVisibility(View.VISIBLE);
			myImageView4.setVisibility(View.VISIBLE);
		}

	}

	// 人脸检测线程
	Runnable FaceThread = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread);
			else {
				nbm = Bitmap.createBitmap(bm);
				/* 人脸检测(人脸双眼间距必须在20pix以上) */
				// 检测器对象
				FaceDetector detector = new FaceDetector(nbm.getWidth(),
						nbm.getHeight(), 1);
				// 存放人脸信息
				FaceDetector.Face[] face = new FaceDetector.Face[1];
				// 人脸数目
				int Number = detector.findFaces(nbm, face);
				if (Number > 0) {
					// 双眼距离
					dist = face[0].eyesDistance();
					// 双眼中点
					face[0].getMidPoint(point);
					/* 归一化裁剪 */
					int x = (int) (point.x - dist);
					int y = (int) (point.y - dist);
					int width = (int) (point.x + dist) - x;
					if (width > nbm.getWidth() - x) {
						width = nbm.getWidth() - x;
					}
					int height = (int) (point.y + dist * 1.5) - y;
					if (height > nbm.getHeight() - y) {
						height = nbm.getHeight() - y;
					}
					nbm = Bitmap.createBitmap(nbm, x, y, width, height);

					// 图像放大至相片尺寸
					Matrix mMatrix = new Matrix();
					float scalex = (float) myCamrActivity.ImageWidth / width;
					float scaley = (float) myCamrActivity.ImageHeight / height;
					mMatrix.postScale(scalex, scaley);
					nbm = Bitmap.createBitmap(nbm, 0, 0, nbm.getWidth(),
							nbm.getHeight(), mMatrix, false);
					bm = nbm;

					// 图片存入SD卡
					File file = new File("./sdcard/mikepic.png");
					BufferedOutputStream bos = null;
					try {
						if (!file.exists())
							file.createNewFile();
						bos = new BufferedOutputStream(new FileOutputStream(
								file));
						bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
						bos.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// logout
					System.out.println(bm.getWidth() + " " + bm.getHeight());

					// 定位眼睛
					Feature.EyesLocate(leftEye, rightEye, dist, point, x, y,
							scalex, scaley);
					// 相关变量归一化变换
					point.x = (point.x - x) * scalex;
					point.y = (point.y - y) * scaley;
					dist = dist * scalex;
				} else {
					// 显示浮动窗口
					Toast.makeText(MainActivity.this, R.string.error,
							Toast.LENGTH_SHORT).show();
					return;
				}
				bar.setProgress(100 / N * 1);
				// myImageView.setImageBitmap(bm);
				updateBarHandler.post(updateThread);
			}
		}

	};

	// 一个测试线程
	Runnable updateThread = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			// 用于结束线程
			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread);
			else {
				/**
				 * 虽然开始时先照相，但是图片暂来自assets文件夹
				 */
				// try {
				// // 从项目中的assets目录下读取图片
				// BufferedInputStream bis = new BufferedInputStream(
				// getAssets().open("mytest.jpg"));
				// // 图片保存在Bitmap对象中
				// bm = BitmapFactory.decodeStream(bis);
				// } catch (Exception e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// // logout
				// System.out.println("file not found");
				// }

				// 进度条
				bar.setProgress(100 / N * 2);
				updateBarHandler.post(updateThread2);
			}
		}

	};

	// 线性灰度变换线程
	Runnable updateThread2 = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread2);
			else {
				// 执行预处理:简单线性灰度变换
				bm = preProcess.linearGray(bm);
				// 装载
				myImageView2.setImageBitmap(bm);
				// 设置进度条
				bar.setProgress(100 / N * 3);
				updateBarHandler.post(updateThread3);
			}
		}

	};

	// 中值滤波线程
	Runnable updateThread3 = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread3);
			else {
				// 执行预处理：中值滤波
				bm = preProcess.medfilter(bm);
				// 增加对比度
				bm = preProcess.incrContrast(bm);
				myImageView3.setImageBitmap(bm);
				// 设置进度条
				bar.setProgress(100 / N * 4);
				updateBarHandler.post(updateThread4);
			}
		}

	};

	// 边缘检测线程
	Runnable updateThread4 = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread4);
			else {
				// 提取边缘(这里有问题：follow方法采用递归形式，运行时造成堆栈溢出，程序异常终止)
				// 解决：让递归执行特定次数，如110次(或者减小图像的尺寸也可以,减少图像尺寸就减少了像素数目) --by mike
				// 参数方面，试了两幅图不同选择对比，低阈值10比较好；高阈值35还算不错
				// 参数：经过无数次照自己，得到的比较好的阈值
				// 阈值对于不同环境效果还是相差很多。。。。。。
				bm = CannyEdgDetect.canny(bm, 5, 40);

				// 设置进度条
				bar.setProgress(100 / N * 5);
				updateBarHandler.post(updateThread5);
			}
		}

	};

	// 特征抽取线程
	Runnable updateThread5 = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (cancel == true)
				updateBarHandler.removeCallbacks(updateThread4);
			else {
				// 提取特征
				// 左眼（之前定位过眼睛中心坐标），伸展参数为STRECH_EYE
				Feature.featurePickUp(leftEye, bm, STRECH_EYE, 2);
				// 左眼边框
				lEyeRect = new Rect(Feature.Left, Feature.Top, Feature.Right,
						Feature.Bottom);
				// 输出
				System.out.println("左：" + Feature.Left);
				System.out.println("右：" + Feature.Right);
				System.out.println("上：" + Feature.Top);
				System.out.println("下：" + Feature.Bottom);

				// 右眼
				Feature.featurePickUp(rightEye, Feature.bmp, STRECH_EYE, 2);
				// 右眼边框
				rEyeRect = new Rect(Feature.Left, Feature.Top, Feature.Right,
						Feature.Bottom);
				// 输出
				System.out.println("左：" + Feature.Left);
				System.out.println("右：" + Feature.Right);
				System.out.println("上：" + Feature.Top);
				System.out.println("下：" + Feature.Bottom);

				// 嘴
				PointF mouse = new PointF(point.x, point.y + dist + LAMDA_a);
				Feature.featurePickUp(mouse, Feature.bmp, STRECH_MOUTH, 4);
				mRect = new Rect(Feature.Left, Feature.Top, Feature.Right,
						Feature.Bottom);
				// 采用面搜索
				for (int i = 1; i < LAMDA_b - LAMDA_a; i++) {
					mouse = new PointF(point.x, point.y + dist + LAMDA_a + i);
					Feature.featurePickUp(mouse, Feature.bmp, STRECH_MOUTH, 3);
					if (mRect.left > Feature.Left)
						mRect.left = Feature.Left;
					if (mRect.right < Feature.Right)
						mRect.right = Feature.Right;
					if (mRect.top > Feature.Top)
						mRect.top = Feature.Top;
					if (mRect.bottom < Feature.Bottom)
						mRect.bottom = Feature.Bottom;
				}
				// 输出
				System.out.println(mRect.left);
				System.out.println(mRect.right);
				System.out.println(mRect.top);
				System.out.println(mRect.bottom);

				// 鼻子
				PointF nose = new PointF(point.x, point.y + dist * 2 / 3
						+ BETA_a);
				Feature.featurePickUp(nose, Feature.bmp, STRECH_NOSE,
						STRECH_NOSE);
				pt_nose = new Point(Feature.Left, Feature.Right);
				// 采用面搜索
				for (int i = 1; i < BETA_b - BETA_a; i++) {
					nose = new PointF(point.x, point.y + dist * 2 / 3 + BETA_a
							+ i);
					Feature.featurePickUp(nose, Feature.bmp, STRECH_NOSE,
							STRECH_NOSE);
					if (pt_nose.x > Feature.Left)
						pt_nose.x = Feature.Left;
					if (pt_nose.y < Feature.Right)
						pt_nose.y = Feature.Right;
				}
				System.out.println("nose:");
				System.out.println(pt_nose.x);
				System.out.println(pt_nose.y);

				boolean flag = Feature.ConstructVector(lEyeRect, rEyeRect,
						mRect, pt_nose, Feature.MATCH);
				if (flag) {
					// 显示浮动窗口
					Toast.makeText(MainActivity.this, R.string.match,
							Toast.LENGTH_SHORT).show();
				} else {
					// 显示浮动窗口
					Toast.makeText(MainActivity.this, R.string.nomatch,
							Toast.LENGTH_SHORT).show();
				}

				// 画边框,查看用
				nbm = DrawBoundry(nbm, lEyeRect);
				nbm = DrawBoundry(nbm, rEyeRect);
				nbm = DrawBoundry(nbm, mRect);
				Rect noseRect = new Rect(pt_nose.x, (int) (point.y + dist * 2
						/ 3 + BETA_a), pt_nose.y,
						(int) (point.y + dist * 2 / 3 + BETA_b));
				nbm = DrawBoundry(nbm, noseRect);
				myImageView.setImageBitmap(nbm);

				// 设置进度条
				bar.setProgress(100);
				// debug
				myImageView4.setImageBitmap(Feature.bmp);

				// 图片存入SD卡
				File file = new File("./sdcard/mikepicCanny.png");
				BufferedOutputStream bos = null;
				try {
					if (!file.exists())
						file.createNewFile();
					bos = new BufferedOutputStream(new FileOutputStream(file));
					Feature.bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
					bos.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				cancelButton.setVisibility(View.GONE);
				exitButton.setVisibility(View.VISIBLE);
				lookButton.setVisibility(View.VISIBLE);
				finishAnalyse = true;
			}
		}

	};

	// 菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// 添加菜单项
		menu.add(0, MENU_SAVE, 0, R.string.menu_save);
		menu.add(0, MENU_ABOUT, 1, R.string.menu_about);
		return true;
	}

	// 菜单项
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 按下MENU_SAVE
		if (item.getItemId() == MENU_SAVE) {
			if (finishAnalyse) {
				Feature.ConstructVector(lEyeRect, rEyeRect, mRect, pt_nose,
						Feature.STOREAGE);
				// 显示浮动窗口
				Toast.makeText(MainActivity.this, R.string.saveok,
						Toast.LENGTH_SHORT).show();
			} else {
				// 显示浮动窗口
				Toast.makeText(MainActivity.this, R.string.saveerr,
						Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		if (item.getItemId() == MENU_ABOUT) {
			// 显示浮动窗口
			Toast.makeText(MainActivity.this, R.string.about,
					Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// 画矩形轮廓,查看用
	private Bitmap DrawBoundry(Bitmap image, Rect rect) {
		Canvas canvas = new Canvas(image);
		// 产生一个红色的矩形边框
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.RED);
		canvas.drawRect(rect, paint);
		return image;
	}

}