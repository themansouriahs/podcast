package org.bottiger.podcast.utils;


import java.net.MalformedURLException;
import java.net.URL;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddPodcastDialog {

	private static final String TAG = "AddPodcastDialog";

	@TargetApi(11)
	@SuppressLint("NewApi")
	public static void addPodcast(final Activity activity) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);

		View alertView = activity.getLayoutInflater().inflate(
				R.layout.add_podcast_dialog, null);
		alertBuilder.setView(alertView);

		alertBuilder.setTitle("Add podcast");

		// Set an EditText view to get user input
		// final EditText input = new EditText(this);
		final EditText input = (EditText) alertView
				.findViewById((R.id.podcast_url));

		// Only run if the API version if above 11
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= 11) {

			ClipboardManager clipboard = (ClipboardManager) activity
					.getApplicationContext().getSystemService(
							activity.getApplicationContext().CLIPBOARD_SERVICE);
			if (clipboard.hasPrimaryClip()) {
				/*
				CharSequence clipText = clipboard.getText();
				if (clipText != null && clipText != "") {
					input.setText(clipText);
				}
				*/
				String clipText  = clipboard.getText().toString();
				try {
					URL url = new URL(clipText);
					input.setText(clipText);
				} catch (MalformedURLException e) {
				}
			}
		}

		alertBuilder.setView(input);

		alertBuilder.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {

						try {
							URL url = new URL(input.getText().toString());
							Subscription sub = new Subscription(url.toString());
							sub.subscribe(activity.getApplicationContext());
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return;
					}
				});

		alertBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				});

		AlertDialog dialog = alertBuilder.create();
		dialog.show();

		if (input.getText().toString().equals("")) {
			Button b = dialog.getButton(DialogInterface.BUTTON1);
			b.setEnabled(false);
			input.setSelection(0);
		}
		input.addTextChangedListener(getTextWatcher(dialog));

		input.setFocusableInTouchMode(true);
		input.requestFocus();
	}

	private static TextWatcher getTextWatcher(final AlertDialog dialog) {
		// second, we create the TextWatcher
		return new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i,
					int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1,
					int i2) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				String text = s.toString();
				try {
					URL url = new URL(text);
					dialog.getButton(DialogInterface.BUTTON1).setEnabled(true);
				} catch (MalformedURLException e) {
					dialog.getButton(DialogInterface.BUTTON1).setEnabled(false);
				}
			}
		};
	}
}
