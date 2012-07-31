package yuku.ambilwarna;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import de.ub0r.android.smsdroid.R;

public class AmbilWarnaDialog {
	private static final String TAG = AmbilWarnaDialog.class.getSimpleName();

	public interface OnAmbilWarnaListener {
		void onReset(AmbilWarnaDialog dialog);

		void onCancel(AmbilWarnaDialog dialog);

		void onOk(AmbilWarnaDialog dialog, int color);
	}

	AlertDialog dialog;
	OnAmbilWarnaListener listener;
	View viewHue;
	AmbilWarnaKotak viewKotak;
	ImageView panah;
	View viewWarnaLama;
	View viewWarnaBaru;
	ImageView viewKeker;

	float satudp;
	int warnaLama;
	int warnaBaru;
	float hue;
	float sat;
	float val;
	float ukuranUiDp = 240.f;
	float ukuranUiPx; // diset di constructor

	public AmbilWarnaDialog(final Context context, final int color,
			final OnAmbilWarnaListener listener) {
		this.listener = listener;
		this.warnaLama = color;
		this.warnaBaru = color;
		Color.colorToHSV(color, this.tmp01);
		this.hue = this.tmp01[0];
		this.sat = this.tmp01[1];
		this.val = this.tmp01[2];

		this.satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
		this.ukuranUiPx = this.ukuranUiDp * this.satudp;
		Log.d(TAG, "satudp = " + this.satudp);
		Log.d(TAG, "ukuranUiPx=" + this.ukuranUiPx);

		View view = LayoutInflater.from(context).inflate(R.layout.ambilwarna_dialog, null);
		this.viewHue = view.findViewById(R.id.ambilwarna_viewHue);
		this.viewKotak = (AmbilWarnaKotak) view.findViewById(R.id.ambilwarna_viewKotak);
		this.panah = (ImageView) view.findViewById(R.id.ambilwarna_panah);
		this.viewWarnaLama = view.findViewById(R.id.ambilwarna_warnaLama);
		this.viewWarnaBaru = view.findViewById(R.id.ambilwarna_warnaBaru);
		this.viewKeker = (ImageView) view.findViewById(R.id.ambilwarna_keker);

		this.letakkanPanah();
		this.letakkanKeker();
		this.viewKotak.setHue(this.hue);
		this.viewWarnaLama.setBackgroundColor(color);
		this.viewWarnaBaru.setBackgroundColor(color);

		this.viewHue.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {

					float y = event.getY(); // dalam px, bukan dp
					if (y < 0.f) {
						y = 0.f;
					}
					if (y > AmbilWarnaDialog.this.ukuranUiPx) {
						y = AmbilWarnaDialog.this.ukuranUiPx - 0.001f;
					}

					AmbilWarnaDialog.this.hue = 360.f - 360.f / AmbilWarnaDialog.this.ukuranUiPx
							* y;
					if (AmbilWarnaDialog.this.hue == 360.f) {
						AmbilWarnaDialog.this.hue = 0.f;
					}

					AmbilWarnaDialog.this.warnaBaru = AmbilWarnaDialog.this.hitungWarna();
					// update view
					AmbilWarnaDialog.this.viewKotak.setHue(AmbilWarnaDialog.this.hue);
					AmbilWarnaDialog.this.letakkanPanah();
					AmbilWarnaDialog.this.viewWarnaBaru
							.setBackgroundColor(AmbilWarnaDialog.this.warnaBaru);

					return true;
				}
				return false;
			}
		});
		this.viewKotak.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {

					float x = event.getX(); // dalam px, bukan dp
					float y = event.getY(); // dalam px, bukan dp

					if (x < 0.f) {
						x = 0.f;
					}
					if (x > AmbilWarnaDialog.this.ukuranUiPx) {
						x = AmbilWarnaDialog.this.ukuranUiPx;
					}
					if (y < 0.f) {
						y = 0.f;
					}
					if (y > AmbilWarnaDialog.this.ukuranUiPx) {
						y = AmbilWarnaDialog.this.ukuranUiPx;
					}

					AmbilWarnaDialog.this.sat = (1.f / AmbilWarnaDialog.this.ukuranUiPx * x);
					AmbilWarnaDialog.this.val = 1.f - (1.f / AmbilWarnaDialog.this.ukuranUiPx * y);

					AmbilWarnaDialog.this.warnaBaru = AmbilWarnaDialog.this.hitungWarna();
					// update view
					AmbilWarnaDialog.this.letakkanKeker();
					AmbilWarnaDialog.this.viewWarnaBaru
							.setBackgroundColor(AmbilWarnaDialog.this.warnaBaru);

					return true;
				}
				return false;
			}
		});

		this.dialog = new AlertDialog.Builder(context)
				.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						if (AmbilWarnaDialog.this.listener != null) {
							AmbilWarnaDialog.this.listener.onOk(AmbilWarnaDialog.this,
									AmbilWarnaDialog.this.warnaBaru);
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						if (AmbilWarnaDialog.this.listener != null) {
							AmbilWarnaDialog.this.listener.onCancel(AmbilWarnaDialog.this);
						}
					}
				}).setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						if (AmbilWarnaDialog.this.listener != null) {
							AmbilWarnaDialog.this.listener.onReset(AmbilWarnaDialog.this);
						}
					}
				}).create();

	}

	@SuppressWarnings("deprecation")
	protected void letakkanPanah() {
		float y = this.ukuranUiPx - (this.hue * this.ukuranUiPx / 360.f);
		if (y == this.ukuranUiPx) {
			y = 0.f;
		}

		AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) this.panah
				.getLayoutParams();
		layoutParams.y = (int) (y + 4);
		this.panah.setLayoutParams(layoutParams);
	}

	@SuppressWarnings("deprecation")
	protected void letakkanKeker() {
		float x = this.sat * this.ukuranUiPx;
		float y = (1.f - this.val) * this.ukuranUiPx;

		AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) this.viewKeker
				.getLayoutParams();
		layoutParams.x = (int) (x + 3);
		layoutParams.y = (int) (y + 3);
		this.viewKeker.setLayoutParams(layoutParams);
	}

	float[] tmp01 = new float[3];

	private int hitungWarna() {
		this.tmp01[0] = this.hue;
		this.tmp01[1] = this.sat;
		this.tmp01[2] = this.val;
		return Color.HSVToColor(this.tmp01);
	}

	public void show() {
		this.dialog.show();
	}
}
