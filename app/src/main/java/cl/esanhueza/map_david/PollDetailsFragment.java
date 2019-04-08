package cl.esanhueza.map_david;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.storage.PersonContract;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;


public class PollDetailsFragment extends Fragment {
    static final int TAKE_POLL = 300;
    final static int PICKFOLDER_REQUEST_CODE = 9000;
    final static int WRITE_REQUEST_CODE = 5000;
    final static int WRITE_REQUEST_CODE_CSV = 5001;
    ResponseDbHelper mDbHelper;
    Poll poll;

    public static PollDetailsFragment newInstance(Poll poll) {
        Bundle args = new Bundle();
        args.putString("POLL", poll.getPath());
        PollDetailsFragment fragment = new PollDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        Bundle bundle = getArguments();
        if (bundle != null){
            String pollString = bundle.getString("POLL");
            mDbHelper = new ResponseDbHelper(getContext());// Gets the data repository in write mode
            view =  inflater.inflate(R.layout.activity_poll_details, null);
            poll = PollFileStorageHelper.readPoll(pollString);

            TextView viewTitle = view.findViewById(R.id.polltitle);
            viewTitle.setText(poll.getTitle());

            TextView viewDescription = view.findViewById(R.id.polldescription);
            viewDescription.setText(poll.getDescription());

            TextView viewAnswers = view.findViewById(R.id.poll_answers);
            viewAnswers.setText(String.valueOf(0));

            TextView viewQuestions = view.findViewById(R.id.poll_question_number);
            viewQuestions.setText(String.valueOf(poll.getQuestions().size()));

            loadStats(view);

            FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPoll(null);
                }
            });

            String personId = checkOpenPoll();
            if (personId != null){
                restartPoll(personId);
            }
            Button btn = view.findViewById(R.id.btn_export_responses);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onActionButtonClick(v);
                }
            });
            Button btn2 = view.findViewById(R.id.btn_export_responses_csv);
            btn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onActionButtonClick(v);
                }
            });
            Button btn3 = view.findViewById(R.id.btn_remove_responses);
            btn3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onActionButtonClick(v);
                }
            });
        }
        else{
            view =  inflater.inflate(R.layout.fragment_poll_details_blank, null);
        }

        return view;
    }

    public void onActionButtonClick(View item) {
        switch (item.getId()) {
            case R.id.btn_remove_responses:
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_poll_delete_responses)
                        .setMessage(R.string.text_poll_delete_responses_more)
                        .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeResponsesFromDb();
                                loadStats();
                            }
                        })
                        .show();
                break;
            case R.id.btn_export_responses:
                String fileName = poll.getTitle();
                fileName = fileName.replace(" ", "_");
                createFile("text/plain", fileName + "_" + poll.getId() + ".json");
                break;
            case R.id.btn_export_responses_csv:
                String csvFileName = poll.getTitle();
                fileName = csvFileName .replace(" ", "_");
                createFileCSV("text/csv", fileName + "_" + poll.getId() + ".csv");
                break;
        }
    }

    private void restartPoll(final String personId){
        new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.text_poll_recovery)
                .setMessage(R.string.text_poll_recovery_more)
                .setCancelable(false)
                .setNegativeButton(R.string.label_button_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removePollNotCompleted(personId);
                        loadStats();
                    }
                })
                .setPositiveButton(R.string.label_button_recover, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startPoll(personId);
                    }
                })
                .show();
    }

    /* recupera una respuesta */
    private String checkOpenPoll() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Insert the new row, returning the primary key value of the new row

        Cursor cursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID,
                },
                PersonContract.PersonEntry.COLUMN_NAME_COMPLETED+ " = ? AND " +
                        PersonContract.PersonEntry.COLUMN_NAME_POLL_ID + " = ?",
                new String[]{
                        String.valueOf(0),
                        poll.getId()
                }, null, null, null);

        String personId = null;
        if(cursor.moveToFirst()){
            personId = cursor.getString(0);
        }
        db.close();
        return personId;
    }

    private void createFileCSV(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE_CSV);
        return;
    }

    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
        return;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            exportResponses(data.getData(), "json");
        }
        else if (requestCode == WRITE_REQUEST_CODE_CSV && resultCode == Activity.RESULT_OK){
            exportResponses(data.getData(), "csv");
        }
        else if (requestCode == TAKE_POLL && resultCode == Activity.RESULT_OK) {
            String personId = data.getStringExtra("personId");
            //exportResponse(personId);
            loadStats();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){

            }
        }
    }

    private void refreshStorage(String path){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(getActivity(), new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                }
            });
        } else {
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + path)));
        }
    }


    /* Agrega las respuesta de una persona al archivo con las respuestas */
    private void exportResponse(String personId) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor personsCursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_DATE,
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID,
                        PersonContract.PersonEntry.COLUMN_NAME_LATITUDE,
                        PersonContract.PersonEntry.COLUMN_NAME_LONGITUDE,
                        PersonContract.PersonEntry.COLUMN_NAME_DATE_COMPLETED,
                },
                PersonContract.PersonEntry.COLUMN_NAME_POLL_ID + "= ? AND " +
                         PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID+ "= ?",
                new String[]{
                        poll.getId(),
                        personId
                },
                null,
                null,
                null);

        Cursor responsesCursor = db.query(ResponseContract.ResponseEntry.TABLE_NAME,
                new String[]{
                        ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID,
                        ResponseContract.ResponseEntry.COLUMN_NAME_CONTENT,
                        ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID
                },
                ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID + "= ? AND " +
                         ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + "= ?",
                new String[]{
                        poll.getId(),
                        personId
                },
                null,
                null,
                null);

        JSONArray responses = new JSONArray();

        try {
            while (personsCursor.moveToNext()) {
                JSONObject personObj = new JSONObject();
                personObj.put("start", personsCursor.getString(0));
                personObj.put("end", personsCursor.getString(4));
                personObj.put("idPersona", personId);
                JSONObject position = new JSONObject();
                if (personsCursor.getString(2) != null){
                    position.put("latitude", personsCursor.getString(2));
                    position.put("longitude", personsCursor.getString(3));
                    personObj.put("position", position);
                }
                JSONArray personResponses = new JSONArray();
                Log.d("TST ENCUESTAS: ", String.valueOf(responsesCursor.getCount()));
                while (responsesCursor.moveToNext()){
                    Log.d("TST ENCUESTAS: ", "Columnas : " + String.valueOf(responsesCursor.getColumnCount()));
                    JSONObject responseObj = new JSONObject(responsesCursor.getString(1));
                    responseObj.put("idPregunta", responsesCursor.getString(2));
                    personResponses.put(responseObj);
                    responsesCursor.moveToNext();
                }
                personObj.put("respuestas", personResponses);
                responses.put(personObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            personsCursor.close();
            responsesCursor.close();
            Log.d("TST ENCUESTAS: ", responses.toString());
            //PollFileStorageHelper.saveResponses(poll, responses);
        }
    }

    private void exportResponses(Uri uriDestination, String format){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor personsCursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_DATE,
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID,
                        PersonContract.PersonEntry.COLUMN_NAME_LATITUDE,
                        PersonContract.PersonEntry.COLUMN_NAME_LONGITUDE,
                        PersonContract.PersonEntry.COLUMN_NAME_DATE_COMPLETED,
                },
                PersonContract.PersonEntry.COLUMN_NAME_POLL_ID + "= ?",
                new String[]{poll.getId()},
                null,
                null,
                ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + " ASC");

        Cursor responsesCursor = db.query(ResponseContract.ResponseEntry.TABLE_NAME,
                new String[]{
                        ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID,
                        ResponseContract.ResponseEntry.COLUMN_NAME_CONTENT,
                        ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID
                },
                ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID + "= ?",
                new String[]{poll.getId()},
                null,
                null,
                ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + " ASC");

        JSONArray responses = new JSONArray();

        try {
            while (personsCursor.moveToNext()) {
                String personId = personsCursor.getString(1);
                JSONObject personObj = new JSONObject();
                personObj.put("start", personsCursor.getString(0));
                personObj.put("end", personsCursor.getString(4));
                //personObj.put("idPersona", personId);

                JSONObject position = new JSONObject();
                if (personsCursor.getString(2) != null){
                    position.put("latitude", personsCursor.getDouble(2));
                    position.put("longitude", personsCursor.getDouble(3));
                    personObj.put("position", position);
                }

                JSONArray personResponses = new JSONArray();
                String responsePersonId = null;
                responsesCursor.moveToFirst();
                do{
                    responsePersonId = responsesCursor.getString(0);
                    JSONObject responseObj = new JSONObject(responsesCursor.getString(1));

                    responseObj.put("idPregunta", responsesCursor.getString(2));

                    personResponses.put(responseObj);
                    responsesCursor.moveToNext();
                }while (responsePersonId.equals(personId) && !responsesCursor.isAfterLast());

                personObj.put("respuestas", personResponses);
                responses.put(personObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            personsCursor.close();
            responsesCursor.close();

            boolean result = false;
            if (format.equals("csv")){
                result = PollFileStorageHelper.saveResponsesCSV(getActivity(), uriDestination, poll, responses);
            }
            else{
                Log.d("SAVE RESPOSNES", responses.toString());
                result = PollFileStorageHelper.saveResponses(getActivity(), uriDestination, poll, responses);
            }

            if (result) refreshStorage(uriDestination.getPath());
        }
    }

    /*  */
    private void removePollNotCompleted(String personId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(PersonContract.PersonEntry.TABLE_NAME,
                PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID + "= ?",
                new String[]{
                    personId
                }
        );
        db.delete(
                ResponseContract.ResponseEntry.TABLE_NAME,
                ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID+ "= ?",
                new String[]{
                        personId
                }
        );
        db.close();
    }

    /* si no se completa la encuesta, se eliminan todas las respuestas de la persona. */
    private void removeResponsesFromDb() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(PersonContract.PersonEntry.TABLE_NAME,
                PersonContract.PersonEntry.COLUMN_NAME_POLL_ID+ "= ?",
                new String[]{poll.getId()}
        );
        db.delete(ResponseContract.ResponseEntry.TABLE_NAME,
                ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID + "= ?",
                new String[]{poll.getId()}
        );
        db.close();
    }



    public void startPoll(@Nullable  String personId){
        Intent intent = new Intent(getActivity(), PollActiveActivity.class);
        if (personId != null){
            intent.putExtra("PERSON", personId);
        }
        intent.putExtra("POLL", poll.getPath());
        startActivityForResult(intent, TAKE_POLL);
    }

    /* almacena la respuesta a una pregunta. */
    private void loadStats(View view) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(PersonContract.PersonEntry.TABLE_NAME, new String[]{PersonContract.PersonEntry.COLUMN_NAME_POLL_ID}, PersonContract.PersonEntry.COLUMN_NAME_POLL_ID + "= ?", new String[]{poll.getId()}, null, null, null);
        TextView viewAnswers = view.findViewById(R.id.poll_answers);
        viewAnswers.setText(String.valueOf(cursor.getCount()));
        db.close();
    }

    /* almacena la respuesta a una pregunta. */
    private void loadStats() {
        loadStats(getView());
    }

    @Override
    public void onDestroy() {
        if (mDbHelper != null){
            mDbHelper.close();
        }

        super.onDestroy();
    }
}
