package org.bottiger.podcast;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.webservices.datastore.webplayer.WebPlayerAuthenticator;

import java.text.ParseException;
import java.util.List;

public class WebScannerActivity extends MediaRouterPlaybackActivity {

    private static final String TAG = WebScannerActivity.class.getSimpleName();

    public static final int SCAN_QR_REQUEST = 466;
    private static final String HAS_CAMERA_PERMISSION = "hasPermission";
    private static final String HAS_SCANNED_QR_CODE = "hasScannedCode";

    private static final boolean DO_BEEP = true;

    private DecoratedBarcodeView mDecoratedBarcodeView;
    private BeepManager mBeepManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkCameraPermission();

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
        mDecoratedBarcodeView.pause();
        super.onPause();
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
        Toast.makeText(this, R.string.web_scanner_qr_scanned, Toast.LENGTH_LONG);

        IEpisode episode = SoundWaves.getAppContext(this).getPlaylist().getItem(0);
        try {
            WebPlayerAuthenticator.authenticate(argResult, this, episode);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "ParseError: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Intent result = getResult(true, false);
        setResult(Activity.RESULT_OK, result);

        finish();
    }

    private void checkCameraPermission() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (hasCameraPermission) {
            return;
        }

        Intent result = getResult(hasCameraPermission, false);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private Intent getResult(boolean argHasPermission, boolean argDidScan) {
        Intent intent = new Intent();
        intent.putExtra(HAS_CAMERA_PERMISSION, argHasPermission);
        intent.putExtra(HAS_SCANNED_QR_CODE, argDidScan);

        return intent;
    }

}
