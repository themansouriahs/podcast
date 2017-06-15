package org.bottiger.podcast.activities.openopml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.utils.SDCardManager;

import java.io.IOException;

import static org.bottiger.podcast.SubscriptionsFragment.RESULT_EXPORT;
import static org.bottiger.podcast.SubscriptionsFragment.RESULT_EXPORT_TO_CLIPBOARD;
import static org.bottiger.podcast.SubscriptionsFragment.RESULT_IMPORT;

public class OPMLImportExportActivity extends ToolbarActivity {

    private static final String TAG = OPMLImportExportActivity.class.getSimpleName();

    /*
    The status codes can be changed, they just need to be the same in all activities, it doesn't matter the number,
        it just matters that reimains the same everywhere.
     */
    private static final int ACTIVITY_CHOOSE_FILE_STATUS_CODE = 99;
    private static final int INTERNAL_STORAGE_PERMISSION_REQUEST = 9;
    private static final String EXTRAS_CODE = "path";
    private static final String EXPORT_RETURN_CODE = "RETURN_EXPORT";
    private static final String MimeType = "file/xml";
    private static final String[] MIME_TYPES = {"file/xml", "application/xml", "text/xml", "text/x-opml", "text/plain"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opml_import_export);

        //PERMISSION CHECK
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Requesting read storage permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, INTERNAL_STORAGE_PERMISSION_REQUEST);
        }

        TextView opml_import_export_text = (TextView) findViewById(R.id.export_opml_text);
        Resources res = getResources();
        String opml_text;
        try {
            String dir = SDCardManager.getExportDir();
            opml_text = String.format(res.getString(R.string.opml_export_explanation_dynamic), dir);

        } catch (IOException e) {
            Log.e(TAG, "Could not access the OPML export dir");
            e.printStackTrace();
            opml_text = res.getString(R.string.opml_export_explanation_dynamic);
        }

        opml_import_export_text.setText(opml_text);

        Toolbar toolbar = (Toolbar) findViewById(R.id.opml_import_export_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.opml_import_export_toolbar_title);
            setSupportActionBar(toolbar);
        }
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) actionbar.setDisplayHomeAsUpEnabled(true);

        //PAST THIS POINT WE ASSUME WE HAVE THE NEEDED PERMISSIO


        /*
            2 Listeners set, one for each button.
            The import one call to startActivityForResult and prompts the user to select the file, then returns to SubscriptionsFragment
                overrided method "onActivityResult"

            The export one returns directly to SubscriptionFragment like the other one
         */
        Button importOPML = (Button) findViewById(R.id.bOPML_import);
        importOPML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "Import OPML clicked");
                Intent chooseFile;
                Intent intent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                //Unless you have an external file manager, this not seems to work, the mimetype is wrong or something

                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                chooseFile.setType("*/*");
                //chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPES);
                //} else {
                //    chooseFile.setType(MimeType);
                //}
                intent = Intent.createChooser(chooseFile, "Choose a file"); //this string is not important since is not shown
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE_STATUS_CODE);
            }
        });


        Button exportOPML = (Button) findViewById(R.id.bOMPLexport);
        exportOPML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "Export OPML to filesystem clicked");
                //The return data is not checked in this particular case
                if (getParent() == null) {
                    setResult(RESULT_EXPORT, null);
                } else {
                    getParent().setResult(RESULT_EXPORT, null);
                }
                finish();
            }
        });

        Button exportClipboardOPML = (Button) findViewById(R.id.bOMPL_clipboard_export);
        exportClipboardOPML.setOnClickListener(new View.OnClickListener() {
            @Nullable
            @Override
            public void onClick(View view) {
                Log.e(TAG, "Export OPML to clipboard clicked");

                //The return data is not checked in this particular case
                if (getParent() == null) {
                    setResult(RESULT_EXPORT_TO_CLIPBOARD, null);
                } else {
                    getParent().setResult(RESULT_EXPORT_TO_CLIPBOARD, null);
                }
                finish();
            }
        });
    }


    //This method is called when the activity resumes from the filesystem selection.

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Operation cancelled because of error or by user");// NoI18N
            //finish();
        }
        if (requestCode == ACTIVITY_CHOOSE_FILE_STATUS_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            Log.e(TAG, "onActivityResult returned RESULT_OK. uri: " + uri);// NoI18N
            //RETURN THE VALUE TO THE APP AND CLOSE
            Intent returnData = new Intent();
            if (getParent() == null) {
                Log.e(TAG, "getParent() == null");// NoI18N
                returnData.setData(uri);
                setResult(RESULT_IMPORT, returnData);
            } else {
                Log.e(TAG, "getParent() != null");// NoI18N
                getParent().setResult(RESULT_IMPORT, data);
            }
            finish();
        }
    }

}
