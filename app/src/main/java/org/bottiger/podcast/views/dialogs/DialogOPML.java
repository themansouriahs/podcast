package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.OPMLImportExport;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogOPML {

    private enum ACTION { IMPORT, EXPORT, NONE};
    private ACTION mAction = ACTION.NONE;


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
                DialogOPML.this.onRadioButtonClicked(ACTION.IMPORT, isChecked);
            }
        });

        exportButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogOPML.this.onRadioButtonClicked(ACTION.EXPORT, isChecked);
            }
        });

        Resources res = argActivity.getResources();
        String opmlImportInstructions = String.format(res.getString(R.string.opml_import_instructions), OPMLImportExport.file.toString());
        textView.setText(opmlImportInstructions);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                OPMLImportExport importExport = new OPMLImportExport(argActivity);
                if (mAction == ACTION.IMPORT) {
                    importExport.importSubscriptions();
                }

                if (mAction == ACTION.EXPORT) {
                    //importExport.e
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

    private void onRadioButtonClicked(ACTION argACTION, boolean isChecked) {
        if (isChecked) {
            mAction = argACTION;
        }
    }
}
