package com.fieldviewer.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.cyrusbowman.opengl.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

public class GLRenderer implements Renderer {

	private MyGLSurfaceView ourSurface;

	public volatile float xAngle;
	public volatile float zAngle;
	public volatile float yTrans;
	public volatile float xTrans;
	public volatile float zWaterPlane;
	public volatile int zWaterPlaneProgress;
	public volatile float zZoom;

	private Triangle tri, tri2;
	private WaterPlane water = null;
	private Context myContext;

	private BitmapLand landImage;
	private LasFile landLas;

	public GLRenderer(Context context, MyGLSurfaceView theSurface) {
		// Cube = new GLCube();
		tri = new Triangle();
		tri2 = new Triangle();
		ourSurface = theSurface;
		landImage = new BitmapLand(ourSurface);
		landLas = new LasFile(ourSurface);

		myContext = context;
		zZoom = 1500.0f;
		xAngle = 0.0f;
		yTrans = 0.0f;
		xTrans = 0.0f;
		zWaterPlane = 0.0f;
		zWaterPlaneProgress = 0;
	}

	public void loadImage(int resourceId, ProgressBar pbar) {
		// landImage.LoadImage(myContext, resourceId, pbar);
		landLas.LoadLas(myContext, resourceId, pbar);
		// landImage.LoadLAS(myContext, resourceId, pbar);
	}

	public void loadLas(int resourceId, ProgressBar pbar) {
		landLas.LoadLas(myContext, resourceId, pbar);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub

		// Less quality more performance
		gl.glDisable(GL10.GL_DITHER);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		// gl.glDepthRangex(0, 1);

		int buf[] = new int[1];

		gl.glGetIntegerv(GL10.GL_DEPTH_BITS, buf, 0);
		Log.d("Buffer bits:", Integer.toString(buf[0]));

		float light0Amb[] = { 0.01f, 0.01f, 0.01f, 1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, light0Amb, 0);
		float light0Diff[] = { 0.5f, 0.5f, 0.5f, 1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, light0Diff, 0);
		float light0Spec[] = { 0.7f, 0.7f, 0.7f, 1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, light0Spec, 0);

		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // background color
		gl.glClearDepthf(20.0f); // background depth
		xAngle = 0.0f; // HERE
		zAngle = 0.0f;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub
		gl.glDisable(GL10.GL_DITHER);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glClearDepthf(20.0f); // background depth
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		// Move camera
		GLU.gluLookAt(gl, 0, 0, -3, 0, 0, 0, 0, 1, 0);
		tri2.setColor(0.9f, 0.1f, 0.0f);
		gl.glTranslatef(0, 0, zZoom); // Zoom out
		tri2.draw(gl);

		gl.glRotatef(xAngle, 1.0f, 0.0f, 0.0f); // Rotate view x angle
		gl.glRotatef(zAngle, 0.0f, 0.0f, 1.0f); // Rotate view y angle
		gl.glTranslatef(-xTrans, -yTrans, 0.0f); // Translate forward

		float light0Pos[] = { 0.0f, 1.0f, 5.0f, 0.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light0Pos, 0);

		float light0Direction[] = { 0.0f, 0.0f, -1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, light0Direction, 0);

		gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 45.0f);

		tri.draw(gl);

		if (landImage.loaded) {
			landImage.draw(gl);
		}
		if (landLas.loaded) {
			landLas.draw(gl);
			if (water == null) {
				water = new WaterPlane(landLas.maxX, landLas.maxY);
			}

			zWaterPlane = landLas.maxZ * (zWaterPlaneProgress * 0.01f) * -1.0f;

			gl.glTranslatef(0, 0, zWaterPlane);
			water.draw(gl);
			gl.glTranslatef(0, 0, -zWaterPlane);

		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		if (height == 0)
			height = 1;
		gl.glViewport(0, 0, width, height);
		float ratio = (float) width / height;

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		// gl.glFrustumf(-ratio, ratio, -1, 1, 3.0f, 20000.0f);
		// GLU.gluPerspective(gl, 45.0f, ratio, 300.0f, 3000.0f); //For 45d
		GLU.gluPerspective(gl, 45.0f, ratio, 200.0f, 1800.0f);
	}
}

class WaterPlane {
	private final FloatBuffer vertexBuffer;

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	float triangleCoords[] = { // in counterclockwise order:
	0.0f, 0.0f, 0.0f, // top
			-2000.0f, 0.0f, 0.0f, // bottom left
			0.0f, 2000.0f, 0.0f, 0.0f, 2000.0f, 0.0f, // top
			-2000.0f, 0.0f, 0.0f, // bottom left
			-2000.0f, 2000.0f, 0.0f // bottom right
	};
	private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
	private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per
															// vertex

	private float blue = (130.0f / 255.0f);
	private float red = 0.0f;
	private float green = (21.0f / 255.0f);

	public WaterPlane(float newMaxX, float newMaxY) {
		triangleCoords[3] = -newMaxX;
		triangleCoords[7] = newMaxY;
		triangleCoords[10] = newMaxY;
		triangleCoords[12] = -newMaxX;
		triangleCoords[15] = -newMaxX;
		triangleCoords[16] = newMaxY;

		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(
		// (number of coordinate values * 4 bytes per float)
				triangleCoords.length * 4);
		// use the device hardware's native byte order
		bb.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		vertexBuffer = bb.asFloatBuffer();
		// add the coordinates to the FloatBuffer
		vertexBuffer.put(triangleCoords);
		// set the buffer to read the first coordinate
		vertexBuffer.position(0);
	}

	public void setColor(float r, float g, float b) {
		blue = b;
		red = r;
		green = g;
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_FRONT);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, vertexStride,
				vertexBuffer);
		gl.glColor4f(red, green, blue, 1.0f);

		gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertexCount);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_CULL_FACE);

	}
}

class Triangle {

	private final FloatBuffer vertexBuffer;

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	static float triangleCoords[] = { // in counterclockwise order:
	0.0f, 0.622008459f, 0.0f, // top
			-0.5f, -0.311004243f, 0.0f, // bottom left
			0.5f, -0.311004243f, 0.0f // bottom right
	};
	private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
	private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per
															// vertex

	private float blue = 0.22265625f;
	private float red = 0.63671875f;
	private float green = 0.76953125f;

	public Triangle() {
		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(
		// (number of coordinate values * 4 bytes per float)
				triangleCoords.length * 4);
		// use the device hardware's native byte order
		bb.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		vertexBuffer = bb.asFloatBuffer();
		// add the coordinates to the FloatBuffer
		vertexBuffer.put(triangleCoords);
		// set the buffer to read the first coordinate
		vertexBuffer.position(0);
	}

	public void setColor(float r, float g, float b) {
		blue = b;
		red = r;
		green = g;
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);
		// gl.glEnable(GL10.GL_CULL_FACE);
		// gl.glCullFace(GL10.GL_BACK);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, vertexStride,
				vertexBuffer);
		gl.glColor4f(red, green, blue, 1.0f);

		gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertexCount);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		// gl.glDisable(GL10.GL_CULL_FACE);

	}
}

class LasFile {
	private renderBuffers renderBuffs;
	private MyGLSurfaceView ourSurface;

	public boolean loaded = false;
	public float maxX = 0.0f;
	public float maxY = 0.0f;
	public float maxZ = 0.0f;

	class renderBuffers {
		public FloatBuffer vertexBuffer;
		public FloatBuffer colorBuffer;
		public ShortBuffer drawListBuffer;
		public int drawListLength; // Size of drawOrder

		public float maxX;
		public float maxY;
		public float maxZ;

		public renderBuffers() {
			vertexBuffer = null;
			drawListBuffer = null;
			colorBuffer = null;
			drawListLength = 0;

			maxX = 0.0f;
			maxY = 0.0f;
			maxZ = 0.0f;
		}
	}

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per
															// vertex

	public LasFile(MyGLSurfaceView theSurface) {
		// renderBuffs = new renderBuffers();
		ourSurface = theSurface;
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glCullFace(GL10.GL_FRONT);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

		// gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, vertexStride,
		// vertexBuffer);

		gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, 0,
				renderBuffs.vertexBuffer);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, renderBuffs.colorBuffer);

		gl.glDrawElements(GL10.GL_TRIANGLES, renderBuffs.drawListLength,
				GL10.GL_UNSIGNED_SHORT, renderBuffs.drawListBuffer);

		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_CULL_FACE);
	}

	public void LoadLas(Context context, int resourceId, ProgressBar pBar) {
		class LoadBackground extends AsyncTask<Void, Integer, renderBuffers> {
			private int resourceId;
			private Context context;
			private Bitmap mapImage = null;
			private ProgressBar theProgress;

			public LoadBackground(int theResourceId, Context theContext,
					ProgressBar pBar) {
				this.resourceId = theResourceId;
				this.context = theContext;
				this.theProgress = pBar;
			}

			public double byteToDouble(byte[] bytes) {
				// convert 8 byte array to double
				int start = 0;
				int i = 0;
				int len = 8;
				int cnt = 0;
				byte[] tmp = new byte[len];
				for (i = start; i < (start + len); i++) {
					tmp[cnt] = bytes[i];
					cnt++;
				}
				long accum = 0;
				i = 0;
				for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
					accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
					i++;
				}
				// long test = (long)( ((bytes[7] & 0xFF) << 56) | ((bytes[6] &
				// 0xFF) << 48) | ((bytes[5] & 0xFF) << 40) | ((bytes[4] & 0xFF)
				// << 32) | ((bytes[3] & 0xFF) << 24) | ((bytes[2] & 0xFF) <<
				// 16) | ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF));
				return Double.longBitsToDouble(accum);
			}

			public int byteToInteger(byte[] bytes) {
				return (int) (((bytes[3] & 0xFF) << 24)
						| ((bytes[2] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF));
			}

			public int byteToShort(byte[] bytes) {
				return (int) ((0x00 << 24) | (0x00 << 16)
						| ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF));
			}

			@Override
			protected renderBuffers doInBackground(Void... params) {
				float overallProgress = 1.0f;
				int countProgress = 0;
				int progressDiv = 0;

				publishProgress(1);
				renderBuffers newRenderBuffs = new renderBuffers();

				List<Float> coords = new ArrayList<Float>();
				List<Short> drawOrder = new ArrayList<Short>();

				InputStream is = context.getResources().openRawResource(
						this.resourceId);

				// Create the byte array to hold the data
				byte[] fileSignature = new byte[4]; // char
				byte[] fileSourceID = new byte[2]; // unsigned short
				byte[] globalEncoding = new byte[2]; // unsigned short
				byte[] GUIDData1 = new byte[4]; // unsigned long
				byte[] GUIDData2 = new byte[2]; // unsigned short
				byte[] GUIDData3 = new byte[2]; // unsigned short
				byte[] GUIDData4 = new byte[8]; // unsigned char
				byte[] versionMajor = new byte[1]; // unsigned char
				byte[] versionMinor = new byte[1]; // unsigned char
				byte[] systemIdentifier = new byte[32]; // char
				byte[] generatingSoftware = new byte[32]; // char
				byte[] creationDayOfYear = new byte[2]; // unsigned short
				byte[] creationYear = new byte[2]; // unsigned short
				byte[] headerSize = new byte[2]; // unsigned short
				byte[] offToPointData = new byte[4]; // unsigned long
				byte[] numVarLengthRecords = new byte[4]; // unsigned long
				byte[] formatId = new byte[1]; // unsigned char
				byte[] recordLength = new byte[2]; // unsigned short
				byte[] numPoints = new byte[4]; // unsigned long
				byte[] numPointsReturn = new byte[20]; // 5 unsigned long's 20
														// bytes, not 28 like
														// doc
				byte[] Xscale = new byte[8]; // double
				byte[] Yscale = new byte[8]; // double
				byte[] Zscale = new byte[8]; // double
				byte[] Xoffset = new byte[8]; // double
				byte[] Yoffset = new byte[8]; // double
				byte[] Zoffset = new byte[8]; // double
				byte[] maxX = new byte[8]; // double
				byte[] minX = new byte[8]; // double
				byte[] maxY = new byte[8]; // double
				byte[] minY = new byte[8]; // double
				byte[] maxZ = new byte[8]; // double
				byte[] minZ = new byte[8]; // double
				double dblMaxZ = 0.0f;
				double dblMinZ = 0.0f;
				int points = 200;

				float maxHeight = 0f;
				float minHeight = -1.0f;

				try {
					is.read(fileSignature);
					ByteBuffer.wrap(fileSignature).getChar();
					is.read(fileSourceID);
					is.read(globalEncoding);
					is.read(GUIDData1);
					is.read(GUIDData2);
					is.read(GUIDData3);
					is.read(GUIDData4);
					int read = is.read(versionMajor);
					Log.d("read:", Integer.toString(read));
					Log.d("versionMajor", Byte.toString(versionMajor[0]));
					is.read(versionMinor);
					Log.d("versionMinor", Byte.toString(versionMinor[0]));
					is.read(systemIdentifier);
					is.read(generatingSoftware);
					is.read(creationDayOfYear);

					Log.d("creationDayOfYear:",
							Integer.toString(byteToShort(creationDayOfYear)));

					is.read(creationYear);

					Log.d("creationYear",
							Integer.toString(byteToShort(creationYear)));

					is.read(headerSize);
					Log.d("headerSize",
							Integer.toString((int) ((0x00 << 24) | (0x00 << 16)
									| ((headerSize[1] & 0xFF) << 8) | (headerSize[0] & 0xFF))));

					is.read(offToPointData);
					int intOffsetToPointData = byteToInteger(offToPointData);
					Log.d("offToPointData",
							Integer.toString(intOffsetToPointData));

					is.read(numVarLengthRecords);

					is.read(formatId);
					Log.d("formatId", Byte.toString(formatId[0]));

					is.read(recordLength);
					int intPointRecordLength = byteToShort(recordLength);
					Log.d("intPointRecordLength",
							Integer.toString(intPointRecordLength));

					is.read(numPoints);
					int intNumberPoints = byteToInteger(numPoints);
					Log.d("numPoints", Integer.toString(intNumberPoints));

					is.read(numPointsReturn);

					is.read(Xscale);
					double dblXscale = byteToDouble(Xscale);
					Log.d("dblXscale", Double.toString(dblXscale));

					is.read(Yscale);
					double dblYscale = byteToDouble(Yscale);
					Log.d("dblYscale", Double.toString(dblYscale));

					is.read(Zscale);
					double dblZscale = byteToDouble(Zscale);
					Log.d("dblZscale", Double.toString(dblZscale));

					is.read(Xoffset);
					double dblXoffset = byteToDouble(Xoffset);
					Log.d("dblXoffset", Double.toString(dblXoffset));

					is.read(Yoffset);
					double dblYoffset = byteToDouble(Yoffset);
					Log.d("dblYoffset", Double.toString(dblYoffset));

					is.read(Zoffset);
					double dblZoffset = byteToDouble(Zoffset);
					Log.d("dblZoffset", Double.toString(dblZoffset));

					is.read(maxX);
					is.read(minX);
					double dblMaxX = byteToDouble(maxX);
					double dblMinX = byteToDouble(minX);

					is.read(maxY);
					is.read(minY);
					double dblMaxY = byteToDouble(maxY);
					double dblMinY = byteToDouble(minY);

					is.read(maxZ);
					is.read(minZ);
					dblMaxZ = byteToDouble(maxZ);
					dblMinZ = byteToDouble(minZ);

					is.close();

					newRenderBuffs.maxX = (float) (dblMaxX - dblMinX);
					newRenderBuffs.maxY = (float) (dblMaxY - dblMinY);
					newRenderBuffs.maxZ = (float) (dblMaxZ - dblMinZ);

					float incrementX = (float) ((dblMaxX - dblMinX) / (float) points);
					float incrementY = (float) ((dblMaxY - dblMinY) / (float) points);
					Log.d("Increments", "IncX:" + Float.toString(incrementX)
							+ " IncY:" + Float.toString(incrementY));
					for (int y = 0; y < points; y++) {
						for (int x = 0; x < points; x++) {
							coords.add(incrementX * x * (-1.0f));
							coords.add(incrementY * y);
							coords.add(0.0f); // Z for now
						}
					}

					is = context.getResources().openRawResource(R.raw.test);
					is.skip(intOffsetToPointData);

					// List<Float> zValue = new ArrayList<Float>();
					float[] zValue = new float[(points * points)];
					int[] zCount = new int[(points * points)];

					int maxCorX = 0;
					int maxCorY = 0;
					int minCorY = points;
					int minCorX = points;

					progressDiv = intNumberPoints / 30;
					countProgress = 0;
					for (int i = 0; i < intNumberPoints; i++) {
						countProgress++;
						if (countProgress > progressDiv) {
							overallProgress += 3.3f;
							publishProgress((int) overallProgress);
							countProgress = 0;
						}
						byte[] byteX = new byte[4]; // long
						byte[] byteY = new byte[4]; // long
						byte[] byteZ = new byte[4]; // long
						is.read(byteX);
						is.read(byteY);
						is.read(byteZ);

						is.skip(10);

						byte[] classification = new byte[1];
						is.read(classification);
						is.skip(intPointRecordLength - 23); // 28 total

						if ((classification[0] & 0x1F) == 0x02 || true) { // It
																			// is
																			// a
																			// ground
																			// point
							int lngX = byteToInteger(byteX);
							// Log.d("lngX", Integer.toString(lngX));
							double dblX = (double) ((lngX * dblXscale) + dblXoffset);
							// Log.d("dblX", Double.toString(dblX));

							int lngY = byteToInteger(byteY);
							// Log.d("lngY", Integer.toString(lngY));
							double dblY = (double) ((lngY * dblYscale) + dblYoffset);

							int coordX = (int) ((dblX - dblMinX) / incrementX);
							int coordY = (int) ((dblY - dblMinY) / incrementY);

							if (coordX > maxCorX) {
								maxCorX = coordX;
							}
							if (coordY > maxCorY) {
								maxCorY = coordY;
							}
							if (coordX < minCorX) {
								minCorX = coordX;
							}
							if (coordY < minCorY) {
								minCorY = coordY;
							}

							int lngZ = byteToInteger(byteZ);
							// Log.d("lngZ", Integer.toString(lngZ));
							double dblZ = (double) ((lngZ * dblZscale) + dblZoffset);

							int theCoord = points * coordY + coordX;
							zValue[theCoord] = (float) (zValue[theCoord] + (dblZ - dblMinZ));
							zCount[theCoord] = zCount[theCoord] + 1;
						}
					}

					progressDiv = points * points / 10;
					countProgress = 0;
					int zCoordIndex = 2;
					for (int i = 0; i < (points * points); i++) { // Average all
																	// heights
																	// for that
																	// point and
																	// set in
																	// coords
						countProgress++;
						if (countProgress > progressDiv) {
							overallProgress += 3.3f;
							publishProgress((int) overallProgress);
							countProgress = 0;
						}
						if (zCount[i] > 0) {
							float avg = (zValue[i] / zCount[i]) * 1.0f; // HERE
							coords.set(zCoordIndex, (avg * -1.0f));
							if (maxHeight < avg) {
								maxHeight = avg;
							}
							if (minHeight > avg || minHeight == -1.0f) {
								minHeight = avg;
							}
						} else {
							coords.set(zCoordIndex,
									(float) ((dblMaxZ - dblMinZ) * -1.0f));
						}
						if (i < 30) {
							Log.d("Coordinates:",
									"Num:"
											+ Integer.toString(i)
											+ " zCount:"
											+ Float.toString(zCount[i])
											+ " zTotal:"
											+ Float.toString(zValue[i])
											+ " x:"
											+ Float.toString(coords
													.get(zCoordIndex - 2))
											+ " y:"
											+ Float.toString(coords
													.get(zCoordIndex - 1))
											+ " z:"
											+ Float.toString(coords
													.get(zCoordIndex)));
						}
						zCoordIndex = zCoordIndex + 3;
					}

					Log.d("minCorX:", Integer.toString(minCorX));
					Log.d("maxCorX:", Integer.toString(maxCorX));
					Log.d("minCorY:", Integer.toString(minCorY));
					Log.d("maxCorY:", Integer.toString(maxCorY));

					// Close the input stream and return bytes
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Divide into 5 colors
				float division = (float) ((maxHeight - minHeight) / 8.0f);

				countProgress = 0;
				progressDiv = points / 10;
				// Find draw order
				for (int x = 0; x < (points - 1); x++) {
					countProgress++;
					if (countProgress > progressDiv) {
						overallProgress += 3.3f;
						publishProgress((int) overallProgress);
						countProgress = 0;
					}
					for (int y = 1; y < points; y++) {
						drawOrder.add((short) (x + (y - 1) * points));
						drawOrder.add((short) (x + 1 + (y - 1) * points));
						drawOrder.add((short) (x + points * y));

						drawOrder.add((short) (x + 1 + (y - 1) * points));
						drawOrder.add((short) (x + 1 + y * points));
						drawOrder.add((short) (x + points * y));
					}

				}

				// Toast.makeText(context,
				// Integer.toString(mapImage.getWidth()),
				// Toast.LENGTH_LONG).show();

				// Initailize buffers

				// (# of coordinate values * 4 bytes per float)
				ByteBuffer bb = ByteBuffer.allocateDirect(coords.size() * 4);
				ByteBuffer colorBB = ByteBuffer
						.allocateDirect((coords.size() + (coords.size() / 3)) * 4);

				bb.order(ByteOrder.nativeOrder());
				colorBB.order(ByteOrder.nativeOrder());

				newRenderBuffs.vertexBuffer = bb.asFloatBuffer();
				newRenderBuffs.colorBuffer = colorBB.asFloatBuffer();

				countProgress = 0;
				progressDiv = coords.size() / 10;
				int which = 1;
				float value = 0.0f;
				for (int i = 0; i < coords.size(); i++) {
					countProgress++;
					if (countProgress > progressDiv) {
						overallProgress += 3.3f;
						publishProgress((int) overallProgress);
						countProgress = 0;
					}

					value = coords.get(i);
					newRenderBuffs.vertexBuffer.put(value);
					if (which == 3) { // Find color of vertex
						which = 0;
						if (value * -1 <= (minHeight + division)) {
							// Red
							newRenderBuffs.colorBuffer.put(24.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(61.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(249.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 2 * division)) {
							// Yellow
							newRenderBuffs.colorBuffer.put(18.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(140.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(250.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 3 * division)) {
							// Green
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(242.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(254.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 4 * division)) {
							// Light blue
							newRenderBuffs.colorBuffer.put(187.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(250.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(112.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 5 * division)) {
							// Light blue
							newRenderBuffs.colorBuffer.put(241.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(250.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(66.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 6 * division)) {
							// Light blue
							newRenderBuffs.colorBuffer.put(255.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(172.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(44.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 7 * division)) {
							// Light blue
							newRenderBuffs.colorBuffer.put(255.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(30.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(22.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= maxHeight) {
							// Blue
							newRenderBuffs.colorBuffer.put(161.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(14.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(9.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else {
							// Black - Should never happen
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						}
					}
					which++;
				}
				newRenderBuffs.vertexBuffer.position(0);
				newRenderBuffs.colorBuffer.position(0);

				// initialize byte buffer for the draw list
				// (# of coordinate values * 2 bytes per short)
				ByteBuffer dlb = ByteBuffer
						.allocateDirect(drawOrder.size() * 2);
				dlb.order(ByteOrder.nativeOrder());
				newRenderBuffs.drawListBuffer = dlb.asShortBuffer();
				for (int i = 0; i < drawOrder.size(); i++) {
					newRenderBuffs.drawListBuffer.put(drawOrder.get(i));
				}
				newRenderBuffs.drawListBuffer.position(0);

				newRenderBuffs.drawListLength = drawOrder.size();

				publishProgress(100);
				return newRenderBuffs;
			}

			protected void onProgressUpdate(Integer... progress) {
				theProgress.setProgress(progress[0]);
			}

			protected void onPostExecute(renderBuffers newBuffers) {
				renderBuffs = newBuffers;

				maxX = renderBuffs.maxX;
				maxY = renderBuffs.maxY;
				maxZ = renderBuffs.maxZ;
				loaded = true;
				ourSurface.requestRender();
			}
		}
		LoadBackground task = new LoadBackground(resourceId, context, pBar);
		task.execute();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
}

class BitmapLand {
	private renderBuffers renderBuffs;
	private MyGLSurfaceView ourSurface;

	public boolean loaded = false;

	class renderBuffers {
		public FloatBuffer vertexBuffer;
		public FloatBuffer colorBuffer;
		public ShortBuffer drawListBuffer;
		public int drawListLength; // Size of drawOrder

		public renderBuffers() {
			vertexBuffer = null;
			drawListBuffer = null;
			colorBuffer = null;
			drawListLength = 0;
		}
	}

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per
															// vertex

	public BitmapLand(MyGLSurfaceView theSurface) {
		// renderBuffs = new renderBuffers();
		ourSurface = theSurface;
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);
		// gl.glEnable(GL10.GL_CULL_FACE);
		// gl.glCullFace(GL10.GL_FRONT);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

		// gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, vertexStride,
		// vertexBuffer);

		gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, 0,
				renderBuffs.vertexBuffer);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, renderBuffs.colorBuffer);

		gl.glDrawElements(GL10.GL_TRIANGLES, renderBuffs.drawListLength,
				GL10.GL_UNSIGNED_SHORT, renderBuffs.drawListBuffer);

		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		// gl.glDisable(GL10.GL_CULL_FACE);
	}

	public void LoadImage(Context context, int resourceId, ProgressBar pBar) {
		class LoadBackground extends AsyncTask<Void, Integer, renderBuffers> {
			private int resourceId;
			private Context context;
			private Bitmap mapImage = null;
			private ProgressBar theProgress;

			public LoadBackground(int theResourceId, Context theContext,
					ProgressBar pBar) {
				this.resourceId = theResourceId;
				this.context = theContext;
				this.theProgress = pBar;
			}

			@Override
			protected renderBuffers doInBackground(Void... params) {
				float overallProgress = 1.0f;
				publishProgress(1);
				renderBuffers newRenderBuffs = new renderBuffers();

				List<Float> coords = new ArrayList<Float>();
				List<Short> drawOrder = new ArrayList<Short>();

				mapImage = decodeSampledBitmapFromResource(
						context.getResources(), this.resourceId, 150, 150);

				// image.setImageBitmap(mapImage);

				int mapHeight = mapImage.getHeight();
				int mapWidth = mapImage.getWidth();

				int numCoords = mapWidth * mapHeight;
				float avgHeight = 0;
				float maxHeight = 0;
				float minHeight = -1.0f;

				int countProgress = 0;
				int progressDiv = mapWidth / 10;
				// Put coords into array
				for (int x = 0; x < mapWidth; x++) {
					for (int y = 0; y < mapHeight; y++) {
						int red = Color.red(mapImage.getPixel(x, y));
						float height = red / 100.0f;
						float xCoord = x / 10.0f;
						float yCoord = y / 10.0f;
						coords.add(xCoord);
						coords.add(yCoord);
						coords.add(-height);
						avgHeight = avgHeight + height;
						if (height > maxHeight) {
							maxHeight = height;
						}
						if (minHeight == -1.0 || minHeight > height) {
							minHeight = height;
						}
					}
					if (countProgress > progressDiv) {
						overallProgress += 3.33;
						publishProgress((int) overallProgress);
						countProgress = 0;
					}
					countProgress++;
				}
				avgHeight = avgHeight / numCoords;

				// Divide into 5 colors
				float division = maxHeight - minHeight;
				division = division / 5.0f;

				countProgress = 0;
				progressDiv = mapWidth / 10;
				// Find draw order
				for (int x = 0; x < (mapWidth - 1); x++) {
					countProgress++;
					if (countProgress > progressDiv) {
						overallProgress += 3.3f;
						publishProgress((int) overallProgress);
						countProgress = 0;
					}
					for (int y = 1; y < mapHeight; y++) {
						drawOrder.add((short) (x + (y - 1) * mapWidth));
						drawOrder.add((short) (x + 1 + (y - 1) * mapWidth));
						drawOrder.add((short) (x + mapWidth * y));

						drawOrder.add((short) (x + 1 + (y - 1) * mapWidth));
						drawOrder.add((short) (x + 1 + y * mapWidth));
						drawOrder.add((short) (x + mapWidth * y));
					}

				}

				// Toast.makeText(context,
				// Integer.toString(mapImage.getWidth()),
				// Toast.LENGTH_LONG).show();

				// Initailize buffers

				// (# of coordinate values * 4 bytes per float)
				ByteBuffer bb = ByteBuffer.allocateDirect(coords.size() * 4);
				ByteBuffer colorBB = ByteBuffer
						.allocateDirect((coords.size() + (coords.size() / 3)) * 4);

				bb.order(ByteOrder.nativeOrder());
				colorBB.order(ByteOrder.nativeOrder());

				newRenderBuffs.vertexBuffer = bb.asFloatBuffer();
				newRenderBuffs.colorBuffer = colorBB.asFloatBuffer();

				countProgress = 0;
				progressDiv = coords.size() / 10;
				int which = 1;
				float value = 0.0f;
				for (int i = 0; i < coords.size(); i++) {
					countProgress++;
					if (countProgress > progressDiv) {
						overallProgress += 3.3f;
						publishProgress((int) overallProgress);
						countProgress = 0;
					}

					value = coords.get(i);
					newRenderBuffs.vertexBuffer.put(value);
					if (which == 3) { // Find color of vertex
						which = 0;
						if (value * -1 <= (minHeight + division)) {
							// Red
							newRenderBuffs.colorBuffer.put(245.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 2 * division)) {
							// Yellow
							newRenderBuffs.colorBuffer.put(222.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(245.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 3 * division)) {
							// Green
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(245.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(47.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= (minHeight + 4 * division)) {
							// Light blue
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(222.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(245.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else if (value * -1 <= maxHeight) {
							// Blue
							newRenderBuffs.colorBuffer.put(12.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(20.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(245.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						} else {
							// Black - Should never happen
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(0.0f / 255.0f);
							newRenderBuffs.colorBuffer.put(1.0f);
						}
					}
					which++;
				}
				newRenderBuffs.vertexBuffer.position(0);
				newRenderBuffs.colorBuffer.position(0);

				// initialize byte buffer for the draw list
				// (# of coordinate values * 2 bytes per short)
				ByteBuffer dlb = ByteBuffer
						.allocateDirect(drawOrder.size() * 2);
				dlb.order(ByteOrder.nativeOrder());
				newRenderBuffs.drawListBuffer = dlb.asShortBuffer();
				for (int i = 0; i < drawOrder.size(); i++) {
					newRenderBuffs.drawListBuffer.put(drawOrder.get(i));
				}
				newRenderBuffs.drawListBuffer.position(0);

				newRenderBuffs.drawListLength = drawOrder.size();
				publishProgress(100);
				return newRenderBuffs;
			}

			protected void onProgressUpdate(Integer... progress) {
				theProgress.setProgress(progress[0]);
			}

			protected void onPostExecute(renderBuffers newBuffers) {
				renderBuffs = newBuffers;
				loaded = true;
				ourSurface.requestRender();
			}
		}
		LoadBackground task = new LoadBackground(resourceId, context, pBar);
		task.execute();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
}
