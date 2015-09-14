package org.bottiger.podcast.views.dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.utils.OPMLImportExport;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogOPML {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IMPORT, EXPORT, NONE})
    public @interface Action {}
    public static final int IMPORT = 0;
    public static final int EXPORT = 1;
    public static final int NONE = 2;

    private @Action int mAction = NONE;


    @RequiresPermission(allOf = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public Dialog onCreateDialog(@NonNull final Activity argActivity) {

        AlertDialog.Builder builder = new AlertDialog.Builder(argActivity);
        // Get the layout inflater
        LayoutInflater inflater = argActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_opml, null);

        TextView textView = (TextView) view.findViewById(R.id.dialog_opml_text);
        RadioButton importButton = (RadioButton) view.findViewById(R.id.radio_import);
        RadioButton exportButton = (RadioButton) view.findViewById(R.id.radio_export);

        importButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogOPML.this.onRadioButtonClicked(IMPORT, isChecked);
            }
        });

        exportButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogOPML.this.onRadioButtonClicked(EXPORT, isChecked);
            }
        });

        Resources res = argActivity.getResources();
        String opmlImportInstructions = String.format(res.getString(R.string.opml_import_instructions), OPMLImportExport.getFilename());
        textView.setText(opmlImportInstructions);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                OPMLImportExport importExport = new OPMLImportExport(argActivity);
                if (mAction == IMPORT) {
                    //importExport.importSubscriptions();
                    new ImportOPMLTask().execute(importExport);
                }

                if (mAction == EXPORT) {
                    importExport.exportSubscriptions();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //DialogOPML.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    private void onRadioButtonClicked(@Action int argACTION, boolean isChecked) {
        if (isChecked) {
            mAction = argACTION;
        }
    }

    private static class ImportOPMLTask extends AsyncTask<OPMLImportExport, Void, Void> {
        protected Void doInBackground(OPMLImportExport... opmlImportExports) {
            opmlImportExports[0].importSubscriptions();
            return null;
        }
    }
}
