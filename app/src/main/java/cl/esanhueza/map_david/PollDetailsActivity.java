package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Text;

import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.storage.PersonContract;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;


public class PollDetailsActivity extends AppCompatActivity {
    static final int TAKE_POLL = 300;
    ResponseDbHelper mDbHelper;
    int pollCount;
    Poll poll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.poll_toolbar);

        setSupportActionBar(myToolbar);

        mDbHelper = new ResponseDbHelper(getApplicationContext());// Gets the data repository in write mode

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String pollString = bundle.getString("POLL");

        try {
            JSONObject pollJson = new JSONObject(pollString);
            poll = new Poll(pollJson);

            setTitle("Detalle de la encuesta");
            TextView viewTitle = findViewById(R.id.polltitle);
            viewTitle.setText(poll.getTitle());

            TextView viewDescription = findViewById(R.id.polldescription);
            viewDescription.setText(poll.getDescription());

            TextView viewAnswers = findViewById(R.id.poll_answers);
            viewAnswers.setText(String.valueOf(0));

            TextView viewQuestions = findViewById(R.id.poll_question_number);
            viewQuestions.setText(String.valueOf(poll.getQuestions().size()));

            loadStats();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPoll();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poll_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_responses:
                removeResponsesFromDb();
                loadStats();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /* Agrega las respuesta de una persona al archivo con las respuestas */
    private void exportResponse(String personId) {
        Log.d("TST ENCUESTAS: ", "(exportResponse) Exportando respuesta de persona: " + personId);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor personsCursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_DATE,
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID
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
                personObj.put("fecha", personsCursor.getString(0));
                personObj.put("idPersona", personId);
                JSONArray personResponses = new JSONArray();


                Log.d("TST ENCUESTAS: ", String.valueOf(responsesCursor.getCount()));
                while (responsesCursor.moveToNext()){
                    Log.d("TST ENCUESTAS: ", "Columans : " + String.valueOf(responsesCursor.getColumnCount()));
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
            PollFileStorageHelper.saveResponses(poll, responses);
        }
    }

    private void exportResponses(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor personsCursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_DATE,
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID
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

        responsesCursor.moveToFirst();

        try {

            while (personsCursor.moveToNext()) {
                String personId = personsCursor.getString(1);
                JSONObject personObj = new JSONObject();
                personObj.put("fecha", personsCursor.getString(0));
                personObj.put("idPersona", personId);
                JSONArray personResponses = new JSONArray();

                String responsePersonId = null;

                Log.d("TST ENCUESTAS: ", String.valueOf(responsesCursor.getCount()));
                do{

                    Log.d("TST ENCUESTAS: ", "Columans : " + String.valueOf(responsesCursor.getColumnCount()));
                    responsePersonId = responsesCursor.getString(0);
                    JSONObject responseObj = new JSONObject(responsesCursor.getString(1));

                    responseObj.put("idPregunta", responsesCursor.getString(2));

                    personResponses.put(responseObj);
                    responsesCursor.moveToNext();
                }while (responsePersonId == personId && !responsesCursor.isLast());
                personObj.put("respuestas", personResponses);
                responses.put(personObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            personsCursor.close();
            responsesCursor.close();
            Log.d("TST ENCUESTAS: ", responses.toString());
            PollFileStorageHelper.saveResponses(poll, responses);
        }
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String personId = data.getStringExtra("personId");
            exportResponse(personId);
            loadStats();
        }
        else{
            Log.d("Result", "Pregunta cerrada sin terminar de contestar.");
        }
    }

    public void startPoll(){
        Intent intent = new Intent(this, PollActiveActivity.class);
        intent.putExtra("POLL", poll.toJson());
        startActivityForResult(intent, TAKE_POLL);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /* almacena la respuesta a una pregunta. */
    private void loadStats() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(PersonContract.PersonEntry.TABLE_NAME, new String[]{PersonContract.PersonEntry.COLUMN_NAME_POLL_ID}, PersonContract.PersonEntry.COLUMN_NAME_POLL_ID + "= ?", new String[]{poll.getId()}, null, null, null);
        TextView viewAnswers = findViewById(R.id.poll_answers);
        viewAnswers.setText(String.valueOf(cursor.getCount()));
        db.close();
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }
}
