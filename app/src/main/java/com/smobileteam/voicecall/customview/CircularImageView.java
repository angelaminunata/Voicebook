package com.smobileteam.voicecall.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircularImageView extends ImageView {
	private int mBorderWidth = 10;
	private int mViewWidth;
	private int mViewHeight;
	private Bitmap mImage;
	private Paint mPaint;
	private Paint mPaintBorder;
	private BitmapShader mShader;

	public CircularImageView(Context context) {
		super(context);
		setup();
	}

	public CircularImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup();
	}

	@SuppressLint("NewApi")
	private void setup() {
		// init paint
		mPaint = new Paint();
		mPaint.setAntiAlias(true);

		mPaintBorder = new Paint();
		setBorderColor(Color.WHITE);
		mPaintBorder.setAntiAlias(true);
		this.setLayerType(LAYER_TYPE_SOFTWARE, mPaintBorder);
//		mPaintBorder.setShadowLayer(4.0f, 0.0f, 2.0f, Color.BLACK); // Set shadow effect
	}

	public void setBorderWidth(int borderWidth) {
		this.mBorderWidth = borderWidth;
		this.invalidate();
	}

	public void setBorderColor(int borderColor) {
		if (mPaintBorder != null)
			mPaintBorder.setColor(borderColor);

		this.invalidate();
	}

	private void loadBitmap() {
		BitmapDrawable bitmapDrawable = (BitmapDrawable) this.getDrawable();

		if (bitmapDrawable != null)
			mImage = bitmapDrawable.getBitmap();
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		// load the bitmap
		loadBitmap();

		if (mImage != null) {
			mShader = new BitmapShader(Bitmap.createScaledBitmap(mImage,
					canvas.getWidth(), canvas.getHeight(), false),
					Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
			mPaint.setShader(mShader);
			int circleCenter = mViewWidth / 2;

			canvas.drawCircle(circleCenter + mBorderWidth, circleCenter
					+ mBorderWidth, circleCenter + mBorderWidth - 4.0f,
					mPaintBorder);
			canvas.drawCircle(circleCenter + mBorderWidth, circleCenter
					+ mBorderWidth, circleCenter - 4.0f, mPaint);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = measureWidth(widthMeasureSpec);
		int height = measureHeight(heightMeasureSpec, widthMeasureSpec);

		mViewWidth = width - (mBorderWidth * 2);
		mViewHeight = height - (mBorderWidth * 2);

		setMeasuredDimension(width, height);
	}

	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Measure the text
			result = mViewWidth;
		}

		return result;
	}

	private int measureHeight(int measureSpecHeight, int measureSpecWidth) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpecHeight);
		int specSize = MeasureSpec.getSize(measureSpecHeight);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = mViewHeight;
		}

		return (result + 2);
	}
}
