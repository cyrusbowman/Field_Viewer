package com.fieldviewer.opengl;

import java.util.ArrayList;
import java.util.List;

import com.fieldviewer.opengl.R;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener,
		OnTouchListener, OnSeekBarChangeListener {

	private MyGLSurfaceView ourSurface;
	private SeekBar seekRain;
	private ProgressBar progress;
	private TextView tvWaterPlaneHeight;

	private float minZoom = 1750.0f;
	private float maxZoom = 250.0f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ourSurface = (MyGLSurfaceView) findViewById(R.id.surfaceviewclass);
		seekRain = (SeekBar) findViewById(R.id.seekRain);
		progress = (ProgressBar) findViewById(R.id.progressBar);
		tvWaterPlaneHeight = (TextView) findViewById(R.id.tvWaterPlaneHeight);

		seekRain.setOnSeekBarChangeListener(this);

		ourSurface.setOnTouchListener(this);

		ourSurface.mRenderer.loadLas(R.raw.aaron, progress); // Load Buckmaster
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		// ImageView theImg = (ImageView) findViewById(R.id.img);
		switch (item.getItemId()) {
		case R.id.lasMap1:
			ourSurface.mRenderer.loadLas(R.raw.test, progress);
			break;
		case R.id.lasMap2:
			ourSurface.mRenderer.loadLas(R.raw.aaron, progress);
			break;
		case R.id.reset:
			ourSurface.mRenderer.xTrans = 0.0f;
			ourSurface.mRenderer.yTrans = 0.0f;
			ourSurface.mRenderer.zAngle = 0.0f;
			ourSurface.mRenderer.zZoom = minZoom;
			ourSurface.requestRender();
			break;
		case R.id.viewAngle:
			if (ourSurface.mRenderer.xAngle > 0.0f) {
				ourSurface.mRenderer.xAngle = 0.0f;
				minZoom = 1750.0f;
				maxZoom = 250.0f;
			} else {
				ourSurface.mRenderer.xAngle = 45.0f;
				minZoom = 1000.0f;
				maxZoom = 310.0f;
				if (ourSurface.mRenderer.zZoom > minZoom) {
					ourSurface.mRenderer.zZoom = minZoom;
				}
			}
			ourSurface.requestRender();
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// ourSurface.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// ourSurface.onResume();
	}

	private class pointer {
		private float xpos;
		private float ypos;
		private float lastX;
		private float lastY;

		private int pointerId;

		public pointer(int id, float startX, float startY) {
			pointerId = id;
			xpos = startX;
			ypos = startY;
			lastX = startX;
			lastY = startY;
		}

		public void setPos(float newX, float newY) {
			lastX = xpos;
			lastY = ypos;
			xpos = newX;
			ypos = newY;
		}

		public void setX(float newX) {
			lastX = xpos;
			xpos = newX;
		}

		public void setY(int newY) {
			lastY = ypos;
			ypos = newY;
		}

		public float getX() {
			return xpos;
		}

		public float getLastX() {
			return lastX;
		}

		public float getY() {
			return ypos;
		}

		public float getLastY() {
			return lastY;
		}

		public int getId() {
			return pointerId;
		}
	}

	private List<pointer> pointers = new ArrayList<pointer>();
	private float lastAngle = 200.0f; // Real value cant be over +-180;
	private float lastDistance = -1.0f;

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		if (arg0.getId() == R.id.surfaceviewclass) {
			// Multitouch on map
			int action = arg1.getActionMasked();
			final int pointerCount = arg1.getPointerCount();
			final int pointerIndex = (arg1.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

			switch (arg1.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				pointer newPointer = new pointer(
						arg1.getPointerId(pointerIndex),
						arg1.getX(pointerIndex), arg1.getY(pointerIndex));
				pointers.add(newPointer);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				for (int i = 0; i < pointers.size(); i++) {
					if (arg1.getPointerId(pointerIndex) == pointers.get(i).pointerId) {
						pointers.remove(i);
					}
				}
				break;
			case MotionEvent.ACTION_MOVE:
				for (int p = 0; p < pointerCount; p++) {
					for (int i = 0; i < pointers.size(); i++) {
						if (arg1.getPointerId(p) == pointers.get(i).pointerId) {
							pointers.get(i).setPos(arg1.getX(p), arg1.getY(p));
						}
					}
				}
				break;
			}

			if (pointers.size() > 1) {
				// Pinch or rotate

				// ROTATE
				// Find angle
				float angle = 0.0f;
				float x0 = pointers.get(0).getX();
				float x1 = pointers.get(1).getX();

				float y0 = pointers.get(0).getY();
				float y1 = pointers.get(1).getY();

				if (x0 != x1) { // Division by zero err on 0 degrees
					angle = (float) Math.toDegrees(Math.atan2((y1 - y0),
							(x1 - x0)));
				}

				// Set two finger angle
				if (lastAngle == 200.0f) {
					lastAngle = angle;
				} else {
					// Rotate the difference
					ourSurface.mRenderer.zAngle += (angle - lastAngle);
					lastAngle = angle;
				}

				// Scale
				float distance = FloatMath
						.sqrt((float) (Math.pow((x1 - x0), 2) + Math.pow(
								(y1 - y0), 2)));
				if (lastDistance == -1.0f) {
					lastDistance = distance;
				} else {
					float diff = lastDistance - distance;
					if ((ourSurface.mRenderer.zZoom + diff) > maxZoom
							&& (ourSurface.mRenderer.zZoom + diff) < minZoom) {
						ourSurface.mRenderer.zZoom += diff;
					}
					lastDistance = distance;
				}
				ourSurface.requestRender();

			} else if (pointers.size() == 1) {
				// Reset 2 finger angle
				if (lastAngle != 200.0f) {
					lastAngle = 200.0f;
				}
				if (lastDistance != -1.0f) {
					lastDistance = -1.0f;
				}

				// Pan
				double sin = Math.sin(Math
						.toRadians(ourSurface.mRenderer.zAngle));
				double cos = Math.cos(Math
						.toRadians(ourSurface.mRenderer.zAngle));
				float dx = pointers.get(0).getX() - pointers.get(0).getLastX();
				float dy = pointers.get(0).getY() - pointers.get(0).getLastY();
				// Need a zoom scaler
				// Forward
				ourSurface.mRenderer.xTrans += dy * 5.0f
						* (ourSurface.mRenderer.zZoom / 2500.0f) * sin;
				ourSurface.mRenderer.yTrans += dy * 5.0f
						* (ourSurface.mRenderer.zZoom / 2500.0f) * cos;

				sin = Math.sin(Math
						.toRadians(ourSurface.mRenderer.zAngle + 90.0f));
				cos = Math.cos(Math
						.toRadians(ourSurface.mRenderer.zAngle + 90.0f));
				ourSurface.mRenderer.xTrans += dx * 5.0f
						* (ourSurface.mRenderer.zZoom / 2500.0f) * sin;
				ourSurface.mRenderer.yTrans += dx * 5.0f
						* (ourSurface.mRenderer.zZoom / 2500.0f) * cos;

				ourSurface.requestRender();
			}

		} else {
			if (arg1.getAction() == MotionEvent.ACTION_DOWN
					|| arg1.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
				switch (arg0.getId()) {

				}
			} else if (arg1.getAction() == MotionEvent.ACTION_UP
					|| arg1.getAction() == MotionEvent.ACTION_POINTER_UP) {
				switch (arg0.getId()) {

				}
			}
		}
		return false;
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		ourSurface.mRenderer.zWaterPlaneProgress = progress;
		ourSurface.requestRender();
		int feet = Math.round((ourSurface.mRenderer.zWaterPlaneMaxHeight * (progress * 0.01f) * 3.28084f));
		tvWaterPlaneHeight.setText(Integer.toString(feet) + " feet");
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

}

class MyGLSurfaceView extends GLSurfaceView {

	public final GLRenderer mRenderer;
	
	public MyGLSurfaceView(Context context, AttributeSet attr) {
		super(context, attr);
		// Set the Renderer for drawing on the GLSurfaceView
		mRenderer = new GLRenderer(this.getContext(), this);
		setRenderer(mRenderer);
		// Render the view only when there is a change in the drawing data
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		// MotionEvent reports input details from the touch screen
		// and other input controls. In this case, you are only
		// interested in events where the touch position changed.

		float x = e.getX();
		float y = e.getY();

		switch (e.getAction()) {
		case MotionEvent.ACTION_MOVE:

			float dx = x - mPreviousX;
			float dy = y - mPreviousY;

			if (y > getHeight() / 2) {
				// dx = dx * -1;
				// dy = dy * -1;
			}

			if (mRenderer.xAngle + (dy) * TOUCH_SCALE_FACTOR > 90.0) {
				// mRenderer.xAngle = 90.0f;
			} else if (mRenderer.xAngle + (dy) * TOUCH_SCALE_FACTOR < 0.0) {
				// mRenderer.xAngle = 0.0f;
			} else {
				// mRenderer.xAngle += (dy) * TOUCH_SCALE_FACTOR; // = 180.0f /
				// 320
			}
			// mRenderer.zAngle += (dx) * TOUCH_SCALE_FACTOR; // = 180.0f / 320

			requestRender();
		}

		mPreviousX = x;
		mPreviousY = y;
		return true;
	}
}
