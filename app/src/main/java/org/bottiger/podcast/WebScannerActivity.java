package org.bottiger.podcast;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.webservices.datastore.webplayer.WebPlayerAuthenticator;

import java.text.ParseException;
import java.util.List;

public class WebScannerActivity extends MediaRouterPlaybackActivity {

    private static final String TAG = "WebScannerActivity";

    private static final boolean DO_BEEP = true;

    private DecoratedBarcodeView mDecoratedBarcodeView;
    private BeepManager mBeepManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView instructions = (TextView) findViewById(R.id.barcode_instructions);
        instructions.setText(fromHtml(getString(R.string.web_scanner_instructions)));

        mBeepManager = new BeepManager(this);

        mDecoratedBarcodeView = (DecoratedBarcodeView) findViewById(R.id.zxing_barcode_scanner);
        mDecoratedBarcodeView.setStatusText("");
        mDecoratedBarcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                Log.d(TAG, "result");
                handleResult(result.getText());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                Log.d(TAG, "result");
            }
        });

        ViewTreeObserver vto = mDecoratedBarcodeView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                          @Override
                                          public void onGlobalLayout() {
                                              int width = mDecoratedBarcodeView.getWidth();
                                              RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mDecoratedBarcodeView.getLayoutParams();
                                              params.height = width;
                                              mDecoratedBarcodeView.setLayoutParams(params);
                                          }
                                      });
    }

    @Override
    public void onResume() {
        super.onResume();
        mDecoratedBarcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDecoratedBarcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_web_scanner, menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected int getLayout() {
        return R.layout.activity_web_scanner;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    private void handleResult(@Nullable String argResult) {

        mBeepManager.playBeepSoundAndVibrate();

        //Toast.makeText(this, "Scanned: " + argResult, Toast.LENGTH_LONG).show();
        //UIUtils.disPlayBottomSnackBar(mDecoratedBarcodeView, R.string.web_scanner_qr_scanned, null, false);
        Toast.makeText(this, R.string.web_scanner_qr_scanned, Toast.LENGTH_LONG);

        IEpisode episode = SoundWaves.getAppContext(this).getPlaylist().getItem(0);
        try {
            WebPlayerAuthenticator.authenticate(argResult, this, episode);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "ParseError: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        finish();
    }

}
