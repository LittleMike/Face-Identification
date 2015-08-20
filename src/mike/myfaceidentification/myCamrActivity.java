package mike.myfaceidentification;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.ImageView;

/**
 * 功能：显示预览图像，在图像上点击则触发拍照操作，拍照成功后显示所拍的照片，然后点击'重拍'菜单项可以重新拍照，点击'OK'菜单项
 * 获取图片，然后进入MainActivity (Mike)
 */

public class myCamrActivity extends Activity implements Callback,
		OnClickListener {

	// 图像大小。暂根据我的手机指定
	public static final int ImageWidth = 240;
	public static final int ImageHeight = 320;
	// 重拍标志
	private static final int MENU_RESTART = 1;
	// 继续标志
	private static final int MENU_OK = 2;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	private Camera mCamera;
	private boolean mPreviewRunning;

	private ImageView mImageView;

	public static Bitmap bitmap = null;

	// 初始化相关资源
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 设置窗口为无标题
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 设置像素格式(在这里需要吗?)
		// this.getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// 设置窗口为全屏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// 装载布局
		setContentView(R.layout.camerlayout);

		// 装载surface控件，图像控件，surfaceHolder对象
		mSurfaceView = (SurfaceView) findViewById(R.id.camera);
		mImageView = (ImageView) findViewById(R.id.image);
		mSurfaceHolder = mSurfaceView.getHolder();

		// 设置surface控件的监听器
		mSurfaceView.setOnClickListener(this);
		// 设置surfaceHolder的回调
		mSurfaceHolder.addCallback(this);
		// 设置mSurfaceHolder的类型(我的手机：安卓2.3.4)(3.0以后不需要)
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	// 当surface创建后初始化Camera对象
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
	}

	// 当surface状态改变的时候，重置Camera的状态，该方法至少运行一次
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}
		// 设置参数
		Parameters params = mCamera.getParameters();
		params.setPreviewSize(width, height);
		params.setPictureSize(ImageHeight, ImageWidth);
		mCamera.setParameters(params);
		// 旋转镜头
		mCamera.setDisplayOrientation(90);

		try {
			// 设置surface用于预览
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mCamera.startPreview();
		// 设置预览状态为开
		mPreviewRunning = true;
	}

	// 当在surface上点击的时候拍照
	public void onClick(View v) {
		mCamera.takePicture(mShutterCallback, null, mPictureCallback);
	}

	/**
	 * 在相机快门关闭时候的回调接口，通过这个接口来通知用户快门关闭的事件，
	 * 普通相机在快门关闭的时候都会发出响声，根据需要可以在该回调接口中定义各种动作， 例如：使设备震动
	 */
	ShutterCallback mShutterCallback = new ShutterCallback() {

		public void onShutter() {

		}

	};

	/**
	 * 拍照的回调接口
	 */
	// 读取所拍的照片并显示
	PictureCallback mPictureCallback = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			if (data != null) {
				// 解析数据为位图
				bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

				// 矩阵，用于旋转
				Matrix matrix = new Matrix();
				// 设置图像的旋转角度
				matrix.setRotate(90);
				// 旋转图像，并生成新的位图对像
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
						bitmap.getHeight(), matrix, false);

				// 在图像控件中载入位图
				mImageView.setImageBitmap(bitmap);
				// 设置控件为可见
				mImageView.setVisibility(View.VISIBLE);
				// 隐藏surface控件
				mSurfaceView.setVisibility(View.GONE);
				// 停止预览状态
				if (mPreviewRunning) {
					mCamera.stopPreview();
					mPreviewRunning = false;
				}
			}
		}

	};

	// 当surface对象销毁的时候释放Camera对象
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPreviewRunning) {
			mCamera.stopPreview();
			mPreviewRunning = false;
		}
		mCamera.release();
	}

	// 菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// 添加菜单项
		menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
		menu.add(0, MENU_OK, 1, R.string.menu_ok);
		return true;
	}

	// 菜单项
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 按下MENU_RESTART
		if (item.getItemId() == MENU_RESTART) {
			mSurfaceView.setVisibility(View.VISIBLE);
			// 重启预览状态
			if (mPreviewRunning) {
				mCamera.stopPreview();
			}
			mCamera.startPreview();
			return true;
		}
		if (item.getItemId() == MENU_OK) {
			if (bitmap != null) {
				if (mPreviewRunning) {
					mCamera.stopPreview();
					mPreviewRunning = false;
				}
				// 生成一个Intent对象
				Intent intent = new Intent();
				// 设置Intent对象要启动的Activity
				intent.setClass(this, MainActivity.class);
				// 通过Intent对象启动另外一个Activity
				this.startActivity(intent);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

}