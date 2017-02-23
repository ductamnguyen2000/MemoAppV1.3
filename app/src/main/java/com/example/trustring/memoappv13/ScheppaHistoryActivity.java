package com.example.trustring.memoappv13;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

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
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ScheppaHistoryActivity extends AppCompatActivity {
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PREFERENCE_NAME = "MyPreferenceAccountName";
    private static final String PREF_CALENDAR_ID = "calendarID";
    private static final String PREF_EVENT_ID = "eventID";
    private static final String TAG = "ABC";
    EventAdapter eventAdapter;
    GoogleAccountCredential mCredential;
    private static String CalendarID = null;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR, DriveScopes.DRIVE
    };
    ProgressDialog mProgress;
    private static Boolean checkStatus = false;
    private static Boolean checkDelete = false;
    private static int checkCount = 0;
    private Menu menu;
    GridView event_lists;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("少々お待ちください。");
        Log.d(TAG, "onCreate: ScheppaHistoryActivity");
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        super.onCreate(savedInstanceState);
        eventAdapter.selectedPositions.clear();
        setContentView(R.layout.activity_scheppa_history);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new MakeRequestTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_scheppa_history, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.mnCheck) {
            //Toast.makeText(this, "icon Check is clicked", Toast.LENGTH_SHORT).show();
            if (checkStatus) {
                checkStatus=false;
                eventAdapter.selectedPositions.clear();
                event_lists.setAdapter(eventAdapter);
                menu.findItem(R.id.mnCount).setTitle("");
                checkCount=0;
            } else {
                menu.findItem(R.id.mnCount).setTitle(checkCount + " Selected");
                checkStatus = true;
            }
        }
        if (id == R.id.mnDelete) {
            //Toast.makeText(this, "icon Delete is clicked", Toast.LENGTH_SHORT).show();
            if (checkStatus&&eventAdapter.selectedPositions.size()>0){
                checkDelete=true;
                new MakeRequestTask().execute();

            }else{
                checkStatus=false;
                eventAdapter.selectedPositions.clear();
                event_lists.setAdapter(eventAdapter);
                menu.findItem(R.id.mnCount).setTitle("");
                checkCount=0;
                checkDelete=false;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, Void> {

        MakeRequestTask() {

        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Void doInBackground(Void... params) {
            if (checkDelete){
                deleteData();
            }
            setDatatoList();
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            event_lists = (GridView) findViewById(R.id.events_List);
            event_lists.setAdapter(eventAdapter);

            Log.d(TAG, "onPostExecute: ");
            event_lists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Toast.makeText(ScheppaHistoryActivity.this, eventAdapter.getItem(position).getId(), Toast.LENGTH_SHORT).show();
                    if (checkStatus) {
                        if (eventAdapter.selectedPositions.contains(position)) {
                            eventAdapter.selectedPositions.remove(eventAdapter.selectedPositions.indexOf(position));
                            view.setBackgroundColor(Color.WHITE);
                        } else {
                            eventAdapter.selectedPositions.add(position);
                            view.setBackgroundColor(Color.GREEN);
                        }
                        checkCount = eventAdapter.selectedPositions.size();
                        Log.d(TAG, "onItemClick: Size" + checkCount);
                        menu.findItem(R.id.mnCount).setTitle(checkCount + " Selected");
                    }else{
                        SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PREF_EVENT_ID, eventAdapter.getItem(position).getId());
                    editor.commit();
                    Log.d(TAG, "onItemClick: Task 1 ");
                    Intent intent = new Intent(ScheppaHistoryActivity.this, ScheppaHistoryDetailActivity.class);
                    startActivity(intent);
                    }
                }
            });
            if (checkDelete){
                checkStatus=false;
                eventAdapter.selectedPositions.clear();
                event_lists.setAdapter(eventAdapter);
                menu.findItem(R.id.mnCount).setTitle("");
                checkCount=0;
                checkDelete=false;
            }
            mProgress.hide();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
        }

        private String getIDCalendar() {
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
            if (pref.getString(PREF_CALENDAR_ID, null) != null) {
                return pref.getString(PREF_CALENDAR_ID, null);
            }
            SharedPreferences.Editor editor = pref.edit();
            com.google.api.services.calendar.Calendar service = new Calendar.Builder(httpTransport, jsonFactory, mCredential)
                    .setApplicationName("Android memoappv13").build();

// Iterate through entries in calendar list
            String pageToken = null;
            try {
                do {
                    CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                    List<CalendarListEntry> items = calendarList.getItems();

                    for (CalendarListEntry calendarListEntry : items) {
                        if (calendarListEntry.getSummary().compareTo("Scheppa") == 0) {
                            editor.putString(PREF_CALENDAR_ID, calendarListEntry.getId());
                            editor.commit();
                            return calendarListEntry.getId();
                        }
                    }
                    pageToken = calendarList.getNextPageToken();
                } while (pageToken != null);
            } catch (Exception e) {
                Log.d(TAG, "getIDCalendar: Error " + e.getMessage());
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
            } catch (Exception e) {
                Log.d(TAG, "getIDCalendar: Create New Calendar Error" + e.getMessage() + "  " + java.util.TimeZone.getDefault().getID().toString());
            }
            return "primary";
        }

        private void setDatatoList() {
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
            mCredential.setSelectedAccountName(pref.getString(PREF_ACCOUNT_NAME, null));
            CalendarID = getIDCalendar();

            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            com.google.api.services.calendar.Calendar service = new Calendar.Builder(httpTransport, jsonFactory, mCredential)
                    .setApplicationName("Android memoappv13").build();
            // try to get Events from CalendarID
            try {
                String pageToken = null;
                do {
                    Events events = service.events().list(CalendarID).setPageToken(pageToken).execute();

                    List<Event> items = events.getItems();
                    Collections.reverse(items);
                    eventAdapter = new EventAdapter(getApplicationContext(), R.layout.listview_scheppa_history, items);

                    for (Event event : items) {
                        Log.d(TAG, "setDatatoList: LIST EVENTS" + event.getSummary());
                    }
                    pageToken = events.getNextPageToken();
                } while (pageToken != null);
            } catch (Exception e) {
                Log.d(TAG, "instance initializer: Error " + e.getMessage());
            }

        }
        private void deleteData() {
            Log.d(TAG, "deleteData: ");
            SharedPreferences pref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
            mCredential.setSelectedAccountName(pref.getString(PREF_ACCOUNT_NAME, null));
            CalendarID = getIDCalendar();

            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            com.google.api.services.calendar.Calendar service = new Calendar.Builder(httpTransport, jsonFactory, mCredential)
                    .setApplicationName("Android memoappv13").build();
            // try to get Events from CalendarID

            for (int i=0;i<eventAdapter.selectedPositions.size();i++) {
                try {
                    service.events().delete(CalendarID, eventAdapter.getItem(eventAdapter.selectedPositions.get(i)).getId()).execute();
                } catch (Exception e) {
                    Log.d(TAG, "instance initializer: Error " + e.getMessage());
                }
            }
        }
    }


}
