package org.bottiger.podcast.views.dialogs;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.StrUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class DialogAddPodcast {

	private static final String TAG = "AddPodcastDialog";

	@SuppressLint("NewApi")
	public static void addPodcast(@NonNull  final Activity activity) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);

		View alertView = activity.getLayoutInflater().inflate(
				R.layout.dialog_add_podcast, null);
		alertBuilder.setView(alertView);

        alertBuilder.setTitle(R.string.dialog_title_add_sub);

		// Set an EditText view to get user input
		// final EditText input = new EditText(this);
		final EditText input = (EditText) alertView
				.findViewById((R.id.podcast_url));

		ClipboardManager clipboard = (ClipboardManager) activity
				.getApplicationContext().getSystemService(
						Context.CLIPBOARD_SERVICE);
		if (clipboard.hasPrimaryClip()) {
			String clipText  = clipboard.getPrimaryClip().toString();
			if (StrUtils.isValidUrl(clipText)) {
				input.setText(clipText);
			}
		}

		alertBuilder.setView(input);

		alertBuilder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {

						String inputText = input.getText().toString();

						if (StrUtils.isValidUrl(inputText)) {
							SlimSubscription slimSubscription = null;
							try {
								slimSubscription = new SlimSubscription(new URL(inputText));
								SoundWaves.getAppContext(activity).getLibraryInstance().subscribe(slimSubscription);
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
						}

						return;
					}
				});

		alertBuilder.setNegativeButton(android.R.string.cancel,
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
			Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
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
				String text = s.toString();
				boolean validUrl = StrUtils.isValidUrl(text);
				dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validUrl);
			}
		};
	}
}
