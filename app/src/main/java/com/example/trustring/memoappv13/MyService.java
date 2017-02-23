package com.example.trustring.memoappv13;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.icu.util.TimeZone;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MyService extends Service {
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String  PREF_CALENDAR_ID = "calendarID";
    private static final String PREFERENCE_NAME = "MyPreferenceAccountName";
    Handler handler = new Handler();
    String TAG = "ABC";
    private static final String EXTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
    private static final String[] PROJECTION = new String[]{
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
    };
    private static final String SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR, DriveScopes.DRIVE
    };
    GoogleAccountCredential mCredential;
    String path = "";
    String imageContent = "";
    String statusProgress = "";
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCYXeybOkISWIkvuuux2PwF-qD85r99C84";
    private static final String SHA1 = "1f8c0943c968922f883b83a316fd0e66b7109187";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        super.onCreate();
        // Thread start ============================================================
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                new ContentObserver(handler) {
                    @Override
                    public boolean deliverSelfNotifications() {
                        Log.d(TAG, "deliverSelfNotifications");
                        return super.deliverSelfNotifications();
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                    }

                    @Override
                    public void onChange(boolean selfChange, Uri uri) {

                        Log.d(TAG, "onChange: " + selfChange + ", " + uri.toString());
                        if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_MATCHER)) {
                            Cursor cursor = null;
                            try {
                                cursor = contentResolver.query(uri, PROJECTION, null, null,
                                        SORT_ORDER);
                                Log.d(TAG, "try: 1");
                                if (cursor != null && cursor.moveToFirst()) {
                                    path = cursor.getString(
                                            cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                                    long dateAdded = cursor.getLong(cursor.getColumnIndex(
                                            MediaStore.Images.Media.DATE_ADDED));
                                    long currentTime = System.currentTimeMillis() / 1000;
                                    Log.d(TAG, "path: " + path + ", dateAdded: " + dateAdded +
                                            ", currentTime: " + currentTime);
                                    if (dateAdded == currentTime && path.toLowerCase().contains("screenshots/")) {
                                        SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
                                        mCredential.setSelectedAccountName(pref.getString(PREF_ACCOUNT_NAME,null));
                                        Log.d(TAG, "onChange: mcre------------"+mCredential.getSelectedAccountName());
                                        new MakeRequestTask(mCredential).execute();
//                                        Intent dialogIntent = new Intent(MyService.this, MakeMemoActivity.class);
//                                        dialogIntent.putExtra("path", path);
//                                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                        // dialogIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                        startActivity(dialogIntent);
                                    }


                                    Log.d(TAG, "Main Activiy IF outside");
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "open cursor fail" + e.toString());
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                        super.onChange(selfChange, uri);
                    }
                }
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Drive mServicedrive = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Android memoappv13")
                    .build();
            mServicedrive = new Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Android memoappv13")
                    .build();
            Log.d("ABC", "MakeRequestTask credential ------ " + credential.getSelectedAccountName());
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                Log.d("ABC", "List<String> getDataFromApi() ");
                googleCloudAPI(Uri.fromFile(new java.io.File(path)));
                return null;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            //Intent getputIntent = getIntent();
            //path = getputIntent.getStringExtra("path");
            // Upload file capture to Google Drive
            File fileMetadata = new File();
            File file = new File();
            String urlFile = null;
            if (path != null) {
                try {
                    fileMetadata.setName("Capture -" + System.currentTimeMillis());
                    fileMetadata.setMimeType("image/png");
                    //fileMetadata.setParents(Collections.singletonList("0BwwA4oUTeiV1TGRPeTVjaWRDY1E"));
                    Log.d("ABC", "getDataFromApi: " + path);
                    java.io.File filePath = new java.io.File(path);
                    FileContent mediaContent = new FileContent("image/png", filePath);
                    file = mServicedrive.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();

                    System.out.println("File ID: " + file.getId());
                    urlFile = "https://drive.google.com/file/d/" + file.getId() + "/view?usp=drive_web";
                } catch (Exception e) {
                    statusProgress = "失敗しました。";
                    Log.d(TAG, "getDataFromApi: mServicedrive mCer" + e.getMessage());
                    return null;
                }

            } else return null;
            //End upload
            Log.d("ABC", "getDataFromApi: Image " + path);
            Log.d("ABCD", "getDataFromApi: Bat dau ");
            String[] lines = imageContent.split("\n");
            Event event = new Event()
                    .setSummary("SCHEPPA " + lines[2] + " " + lines[3])
                    .setDescription("【写真の文字】\n【START】" + imageContent + "【END】\n This photo was taken at " + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(java.util.Calendar.getInstance().getTime()) + '\n' + urlFile);

            DateTime startDateTime = new DateTime(new Date());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            event.setStart(start);

            DateTime endDateTime = new DateTime(new Date());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            String calendarId = getIDCalendar(mCredential);
            // Google Drive UP
            try {
                if (urlFile != null) {
                    List<EventAttachment> attachments = new ArrayList<EventAttachment>();

                    attachments.add(new EventAttachment()
                            .setFileUrl(urlFile)
                            .setFileId(file.getId())
                            .setMimeType(file.getMimeType())
                            .setTitle(path));
                    event.setAttachments(attachments);
                    event = mService.events().insert(calendarId, event).setSupportsAttachments(true).execute();
                } else {
                    Log.d("ABCD", "getDataFromApi: khong co anh ");
                    event = mService.events().insert(calendarId, event).execute();
                }
            } catch (Exception e) {
                statusProgress = "失敗しました。";
                Log.d(TAG, "getDataFromApi: mService mCer " + e.getMessage());
                return null;
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());
            statusProgress = "SCHEPPA完了";
            //finish();
            //getputIntent.removeExtra("path");
            return null;
        }

        private void googleCloudAPI(final Uri uri) throws IOException {
            try {
                HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                VisionRequestInitializer requestInitializer =
                        new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                            /**
                             * We override this so we can inject important identifying fields into the HTTP
                             * headers. This enables use of a restricted cloud platform API key.
                             */
                            @Override
                            protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                    throws IOException {
                                super.initializeVisionRequest(visionRequest);

                                String packageName = getPackageName();
                                visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                //String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, SHA1);
                            }
                        };

                Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                builder.setVisionRequestInitializer(requestInitializer);

                Vision vision = builder.build();

                BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                        new BatchAnnotateImagesRequest();
                batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                    // Add the image
                    Image base64EncodedImage = new Image();
                    // Convert the bitmap to a JPEG
                    // Just in case it's a format that Android understands but Cloud Vision
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    scaleBitmapDown(
                            MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                            1200).compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                    byte[] imageBytes = byteArrayOutputStream.toByteArray();

                    // Base64 encode the JPEG
                    base64EncodedImage.encodeContent(imageBytes);
                    annotateImageRequest.setImage(base64EncodedImage);

                    // add the features we want
                    annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
//                        Feature labelDetection = new Feature();
//                        labelDetection.setType("LABEL_DETECTION");
//                        labelDetection.setMaxResults(3);

                        Feature textDetection = new Feature();
                        textDetection.setType("TEXT_DETECTION");

//                        Feature landMarkDetection = new Feature();
//                        textDetection.setType("LANDMARK_DETECTION");

//                        add(labelDetection);
                        add(textDetection);
//                        add(landMarkDetection);
                    }});

                    // Add the list of one thing to the request
                    add(annotateImageRequest);
                }});

                Vision.Images.Annotate annotateRequest =
                        vision.images().annotate(batchAnnotateImagesRequest);
                // Due to a bug: requests to Vision API containing large images fail when GZipped.
                annotateRequest.setDisableGZipContent(true);
                Log.d("ABC", "created Cloud Vision request object, sending request");

                BatchAnnotateImagesResponse response = annotateRequest.execute();
                imageContent = convertResponseToString(response);
                getDataFromApi();
return;
            } catch (GoogleJsonResponseException e) {
                Log.d("ABC", "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d("ABC", "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            Log.d("ABC", "googleCloudAPI: Cloud Vision API request failed. Check logs for details.");
        }

        @Override
        protected void onPreExecute() {
            Log.d("ABC", "onPreExecute: ");
            Toast.makeText(MyService.this, "SCHEPPA中", Toast.LENGTH_SHORT).show();
            // mOutputText.setText("");
            // mProgress.show();
            //mProgress.hide();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            // mProgress.hide();
            if (output == null || output.size() == 0) {
                //  mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                // mOutputText.setText(TextUtils.join("\n", output));

            }
            //mProgress.dismiss();
            Toast.makeText(MyService.this, statusProgress, Toast.LENGTH_SHORT).show();
            Log.d("ABC", "onPostExecute: ");


            // finish();
            // finishAffinity();
            //System.exit(0);
            //android.os.Process.killProcess(android.os.Process.myPid());
        }

        @Override
        protected void onCancelled() {
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    //showGooglePlayServicesAvailabilityErrorDialog(
                    //       ((GooglePlayServicesAvailabilityIOException) mLastError)
                    //              .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    // startActivityForResult(
                    //     ((UserRecoverableAuthIOException) mLastError).getIntent(),
                    //    MakeMemoActivity.REQUEST_AUTHORIZATION);
                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
                //  mOutputText.setText("Request cancelled.");
            }
        }

        private String getIDCalendar (GoogleAccountCredential credentials){
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
            if( pref.getString(PREF_CALENDAR_ID,null) != null){
                return  pref.getString(PREF_CALENDAR_ID,null);
            }
            SharedPreferences.Editor editor = pref.edit();
            com.google.api.services.calendar.Calendar service = new Calendar.Builder(httpTransport, jsonFactory, credentials)
                    .setApplicationName("Android memoappv13").build();

// Iterate through entries in calendar list
            String pageToken = null;
            try {
                do {
                    CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                    List<CalendarListEntry> items = calendarList.getItems();

                    for (CalendarListEntry calendarListEntry : items) {
                        if (calendarListEntry.getSummary().compareTo("Scheppa")==0)
                        {
                            editor.putString(PREF_CALENDAR_ID, calendarListEntry.getId());
                            editor.commit();
                            return calendarListEntry.getId();
                        }
                    }
                    pageToken = calendarList.getNextPageToken();
                } while (pageToken != null);

            } catch (Exception e){
                Log.d(TAG, "getIDCalendar: Error "+e.getMessage());
                return "primary";
            }
            try {
                com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
                calendar.setSummary("Scheppa");
                calendar.setTimeZone(java.util.TimeZone.getDefault().getID().toString());
                com.google.api.services.calendar.model.Calendar createdCalendar = service.calendars().insert(calendar).execute();
                editor.putString(PREF_CALENDAR_ID, createdCalendar.getId());
                editor.commit();
                return createdCalendar.getId();
            }catch (Exception e){
                Log.d(TAG, "getIDCalendar: Create New Calendar Error"+e.getMessage()+"  "+java.util.TimeZone.getDefault().getID().toString());
            }
            return "primary";
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = ""; //"I found these things:\n\n";

//        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
//        if (labels != null) {
//
//            for (EntityAnnotation label : labels) {
//                labelContent += " - "+label.getDescription();
//            }
//        } else {
//            message += "nothing";
//        }
        List<EntityAnnotation> textcont = response.getResponses().get(0).getTextAnnotations();
        message += "\n" + textcont.get(0).getDescription();

//        List<EntityAnnotation> landcont = response.getResponses().get(0).getLandmarkAnnotations();
//        message += "\n LANDMARK"+landcont.get(0).getDescription();
        Log.d(TAG, "convertResponseToString: " + message);
        return message;
    }



}