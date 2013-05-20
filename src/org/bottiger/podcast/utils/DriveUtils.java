package org.bottiger.podcast.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DriveUtils {

	  static final int REQUEST_ACCOUNT_PICKER = 1;
	  static final int REQUEST_AUTHORIZATION = 2;
	  static final int CAPTURE_IMAGE = 3;

	  
	  private GoogleAccountCredential credential;
	  private Activity mActivity;
	  private static Uri fileUri;
	  private static Drive service;

	  
	  public DriveUtils(Activity activity) {
		  this.mActivity = activity;
	  }
	  
	  public void driveAccount() {
		    credential = GoogleAccountCredential.usingOAuth2(mActivity, DriveScopes.DRIVE);
		    mActivity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	  }
	  
	  public void activityResult(final int requestCode, final int resultCode, final Intent data) {
		    switch (requestCode) {
		    case REQUEST_ACCOUNT_PICKER:
		      if (resultCode == mActivity.RESULT_OK && data != null && data.getExtras() != null) {
		        String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		        if (accountName != null) {
		          credential.setSelectedAccountName(accountName);
		          service = getDriveService(credential);
		          //startCameraIntent();
		          updateDatabase();
		        }
		      }
		      break;
		    case REQUEST_AUTHORIZATION:
		      if (resultCode == Activity.RESULT_OK) {
		        //saveFileToDrive();
		      } else {
		    	  mActivity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
		      }
		      break;
		    case CAPTURE_IMAGE:
		      if (resultCode == Activity.RESULT_OK) {
		        //saveFileToDrive();
		      }
		    }
	  }
	  
	  public void updateDatabase() {
		  String inFileName = "/data/data/org.bottiger.podcast/databases/podcast.db";
		  java.io.File dbFile = new java.io.File(inFileName);
		  saveFileToDrive(dbFile, "application/x-sqlite3"); //"application/vnd.google-apps.file");
	  }
	  
	  private Drive getDriveService(GoogleAccountCredential credential) {
		    return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
		        .build();
		  }
	  
	  private void saveFileToDrive(final java.io.File inputFile, final String mineType) {
		    Thread t = new Thread(new Runnable() {
		      @Override
		      public void run() {
		        try {
		          // File's binary content
		          //java.io.File fileContent = new java.io.File(fileUri.getPath());
		          FileContent mediaContent = new FileContent(mineType, inputFile); // "image/jpeg"

		          // File's metadata.
		          File body = new File();
		          String fileTitle = inputFile.getName();
		          body.setTitle(fileTitle);
		          body.setMimeType(mineType);
		          body.setAppDataContents(true);
		          body.setDescription("dbversion: " + 10); //FIXME

		          // http://stackoverflow.com/questions/12811705/create-folder-if-it-does-not-exist-in-the-google-drive
		          // https://developers.google.com/drive/search-parameters
		          Files.List request = service.files().list().setQ(
		        	       "mimeType='" + mineType + "' and title='" + fileTitle + "'");
		          FileList files = request.execute();
		          
		          File file;
		          if (files.getItems().size() > 0) {
		        	  String fileId = files.getItems().get(0).getId();
			          file = service.files().update(fileId, body).execute();
		          } else
		        	  file = service.files().insert(body, mediaContent).execute();
		          
		          if (file != null) {
		        	//mActivity.showToast("Photo uploaded: " + file.getTitle());
		            //startCameraIntent();
		          }
		        } catch (UserRecoverableAuthIOException e) {
		        	mActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
		        } catch (IOException e) {
		          e.printStackTrace();
		        }
		      }
		    });
		    t.start();
		  }
	  
	  private void startCameraIntent() {
		    String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
		        Environment.DIRECTORY_PICTURES).getPath();
		    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		    fileUri = Uri.fromFile(new java.io.File(mediaStorageDir + java.io.File.separator + "IMG_"
		        + timeStamp + ".jpg"));

		    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		    mActivity.startActivityForResult(cameraIntent, CAPTURE_IMAGE);
		  }



	
}
