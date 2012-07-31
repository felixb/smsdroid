package yuku.ambilwarna;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import de.ub0r.android.smsdroid.R;

public class AmbilWarnaKotak extends View {

	Paint paint;
	Shader dalam;
	Shader luar;
	float hue;
	float satudp;
	float ukuranUiDp = 240.f;
	float ukuranUiPx; // diset di constructor
	float[] tmp00 = new float[3];

	public AmbilWarnaKotak(final Context context) {
		this(context, null);
	}

	public AmbilWarnaKotak(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AmbilWarnaKotak(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		this.satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
		this.ukuranUiPx = this.ukuranUiDp * this.satudp;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		if (this.paint == null) {
			this.paint = new Paint();
			this.luar = new LinearGradient(0.f, 0.f, 0.f, this.ukuranUiPx, 0xffffffff, 0xff000000,
					TileMode.CLAMP);
		}

		this.tmp00[1] = this.tmp00[2] = 1.f;
		this.tmp00[0] = this.hue;
		int rgb = Color.HSVToColor(this.tmp00);

		this.dalam = new LinearGradient(0.f, 0.f, this.ukuranUiPx, 0.f, 0xffffffff, rgb,
				TileMode.CLAMP);
		ComposeShader shader = new ComposeShader(this.luar, this.dalam, PorterDuff.Mode.MULTIPLY);

		this.paint.setShader(shader);

		canvas.drawRect(0.f, 0.f, this.ukuranUiPx, this.ukuranUiPx, this.paint);
	}

	void setHue(final float hue) {
		this.hue = hue;
		this.invalidate();
	}
}
