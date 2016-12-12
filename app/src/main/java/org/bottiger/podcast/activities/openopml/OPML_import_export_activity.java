package org.bottiger.podcast.activities.openopml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import org.bottiger.podcast.R;
import org.bottiger.podcast.ToolbarActivity;

public class OPML_import_export_activity extends ToolbarActivity {

    /*
    The status codes can be changed, they just need to be the same in all activities, it doesn't matter the number,
        it just matters that reimains the same everywhere.
     */
    private static final int ACTIVITY_CHOOSE_FILE_STATUS_CODE = 99;
    private static final int INTERNAL_STORAGE_PERMISSION_REQUEST = 9;
    private static final String EXTRAS_CODE = "path";
    private static final String EXPORT_RETURN_CODE = "RETURN_EXPORT";
    private static final String MimeType = "file/xml";
    private static final String TAG = "OPML_io_act";
    private static final int RESULT_IMPORT = 201;
    private static final int RESULT_EXPORT = 202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opml_import_export);

        //PERMISSION CHECK

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, INTERNAL_STORAGE_PERMISSION_REQUEST);

        }

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
                Intent chooseFile;
                Intent intent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                //Unless you have an external file manager, this not seems to work, the mimetype is wrong or something
                chooseFile.setType(MimeType);
                intent = Intent.createChooser(chooseFile, "Choose a file"); //this string is not important since is not shown
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE_STATUS_CODE);
            }
        });


        Button exportOPML = (Button) findViewById(R.id.bOMPLexport);
        exportOPML.setOnClickListener(new View.OnClickListener() {
            @Nullable
            @Override
            public void onClick(View view) {
                //The return data is not checked in this particular case
                if (getParent() == null) {
                    setResult(RESULT_EXPORT, null);
                } else {
                    getParent().setResult(RESULT_EXPORT, null);
                }
                finish();
            }
        });
    }


    //This method is called when the activity resumes from the filesystem selection.

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.d(TAG, "Operation cancelled because of error or by user");// NoI18N
            //finish();
        }
        if (requestCode == ACTIVITY_CHOOSE_FILE_STATUS_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            //RETURN THE VALUE TO THE APP AND CLOSE
            Intent returnData = new Intent();
            if (getParent() == null) {
                returnData.setData(uri);
                setResult(RESULT_IMPORT, returnData);
            } else {
                getParent().setResult(RESULT_IMPORT, data);
            }
            finish();
        }
    }

}
