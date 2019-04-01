package cl.esanhueza.map_david;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Debug;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItem;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.models.Question;
import cl.esanhueza.map_david.storage.PersonContract;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;

public class PollActiveActivity extends CustomActivity {
    static final public HashMap QUESTION_TYPE_LIST = new HashMap<String, java.lang.Class>() {{
        put("polygon", NewPolygonActivity.class);
        put("route", RouteActivity.class);
        put("text", TextActivity.class);
        put("choice", MultipleChoiceActivity.class);
        put("range", RangeActivity.class);
        put("point", PointActivity.class);
        put("point+", PointPlusActivity.class);
    }};

    private Menu mMenu;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private GeoPoint currentLocation;
    private boolean workWithoutLocation = false;

    ArrayList<Question> questionsList = new ArrayList<>();
    ResponseDbHelper mDbHelper;
    Poll poll;
    String personId;

    PollActiveActivity.QuestionAdapter mAdapter;
    ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_active);

        final Toolbar myToolbar = (Toolbar) findViewById(R.id.poll_toolbar);
        setSupportActionBar(myToolbar);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        String pollString = bundle.getString("POLL");

        poll = PollFileStorageHelper.readPoll(pollString);
        setTitle(poll.getTitle());
        questionsList.addAll(poll.getQuestions());

        mDbHelper = new ResponseDbHelper(getApplicationContext());// Gets the data repository in write mode

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        mAdapter = new PollActiveActivity.QuestionAdapter(this, questionsList);
        listView = findViewById(R.id.question_list);
        listView.setAdapter(mAdapter);

        // al dar click sobre una fila, pasar pregunta al editor de preguntas
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openQuestion(questionsList.get(position));
            }
        });

        if (intent.hasExtra("PERSON")){
            personId = intent.getStringExtra("PERSON");
            loadProgress();
        }
        else{
            personId = UUID.randomUUID().toString().replace("-", "");
            insertNewPerson();
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_poll_active, menu);
        mMenu = menu;

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                MenuItem menuItem = (MenuItem) mMenu.getItem(0);
                Drawable drawable = getResources().getDrawable(R.drawable.ic_location_on_black_24dp);
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, getResources().getColor(R.color.colorSuccess));
                menuItem.setIcon(drawable);
                setPositionToDb();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                MenuItem menuItem = (MenuItem) mMenu.getItem(0);
                Drawable drawable = getResources().getDrawable(R.drawable.ic_location_on_black_24dp);
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, getResources().getColor(R.color.colorInfo));
                menuItem.setIcon(drawable);
            }

            @Override
            public void onProviderDisabled(String provider) {
                MenuItem menuItem = (MenuItem) mMenu.getItem(0);
                Drawable drawable = getResources().getDrawable(R.drawable.ic_location_on_black_24dp);
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, getResources().getColor(R.color.colorWarning));
                menuItem.setIcon(drawable);

                new AlertDialog.Builder(PollActiveActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_require_position_title)
                        .setMessage(R.string.text_require_position)
                        .setNegativeButton(R.string.label_button_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                workWithoutLocation = true;
                            }
                        })
                        .setPositiveButton(R.string.label_button_enable_gps, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .show();

            }
        };
        updatePosition();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_update_location:
                updatePosition();
                /*
                //Toast.makeText(PollDetailsActivity.this, "Se ha creado un archivo con los resultados de las encuestas aplicadas.", Toast.LENGTH_SHORT);
                */
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void updatePosition(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, null);
            }
        }
    }

    public void openQuestion(Question q) {
        // se revisa si el tipo de pregunta esta previamente implementado.
        if (!QUESTION_TYPE_LIST.containsKey(q.getType())) {
            Toast.makeText(this, "El tipo de pregunta seleccionado no está implementado.", Toast.LENGTH_LONG).show();
            return;
        }
        java.lang.Class activity = (Class) QUESTION_TYPE_LIST.get(q.getType());
        Intent intent = new Intent(this, activity);
        intent.putExtra("QUESTION", q.toJson());

        // si se recupera una respuesta desde la base de datos, se agrega al intent.
        JSONObject response = loadResponseFromDb(q.getNumber());
        if (response != null){
            intent.putExtra("RESPONSE", response.toString());
        }
        if (currentLocation != null) {
            intent.putExtra("POSITION", "{\"latitude\": " + String.valueOf(currentLocation.getLatitude()) + ", \"longitude\": " + String.valueOf(currentLocation.getLongitude()) + "}");
        }
        startActivityForResult(intent, q.getNumber());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            try {
                JSONObject result = new JSONObject(returnedResult);

                //Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();
                Question q = findQuestionByNumber(requestCode);
                if (q != null) {
                    q.setState("Contestada");
                    saveResponseToDb(q.getNumber(), result);
                    mAdapter.notifyDataSetChanged();
                    checkCompleted();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
        }
    }

    public void checkCompleted() {
        boolean completed = true;
        for (Question q : questionsList) {
            if (q.getState() == "Pendiente") { //&& q.isRequired()
                completed = false;
            }
        }
        if (completed) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Encuesta completada")
                    .setMessage("Ahora podra abandonar la encuesta de forma segura.")
                    .setPositiveButton("Salir", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.putExtra("personId", personId);
                            setResult(Activity.RESULT_OK, intent);
                            setPollCompleted();
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        for (Question q : questionsList) {
            if (q.getState() == "Pendiente") {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_poll_incomplete_title)
                        .setMessage(R.string.text_poll_incomplete)
                        .setPositiveButton(R.string.text_poll_leave_poll, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeResponsesFromDb();
                                setResult(Activity.RESULT_CANCELED);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.label_button_cancel, null)
                        .show();
                return;
            }
        }
        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }

    private void insertNewPerson(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(PersonContract.PersonEntry.COLUMN_NAME_POLL_ID, poll.getId());
        values.put(PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID, personId);

        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        values.put(PersonContract.PersonEntry.COLUMN_NAME_DATE, format.format(currentTime));
        values.put(PersonContract.PersonEntry.COLUMN_NAME_COMPLETED, 0);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(PersonContract.PersonEntry.TABLE_NAME, null, values);
        db.close();
    }

    private void setPositionToDb(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PersonContract.PersonEntry.COLUMN_NAME_LATITUDE, String.valueOf(currentLocation.getLatitude()));
        values.put(PersonContract.PersonEntry.COLUMN_NAME_LONGITUDE, String.valueOf(currentLocation.getLongitude()));

        db.update(PersonContract.PersonEntry.TABLE_NAME, values, PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID + " = ?", new String[]{personId});
        db.close();
    }

    private void setPollCompleted(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        values.put(PersonContract.PersonEntry.COLUMN_NAME_DATE_COMPLETED, format.format(currentTime));
        values.put(PersonContract.PersonEntry.COLUMN_NAME_COMPLETED, 1);

        // Insert the new row, returning the primary key value of the new row
        db.update(PersonContract.PersonEntry.TABLE_NAME, values, PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID + " = ?", new String[]{personId});
        db.close();
    }

    /* recupera una respuesta */
    private ArrayList<String> loadProgress() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the primary key value of the new row
        Cursor cursor = db.query(ResponseContract.ResponseEntry.TABLE_NAME,
                new String[]{
                        ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID
                }, ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + " = ? AND " +
                        ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID+ " = ?",
                new String[]{
                        personId,
                        poll.getId()
                }, null, null, null);

        ArrayList<String> ids = new ArrayList<>();

        while (cursor.moveToNext()){
            ids.add(cursor.getString(0));
        }

        for (Question q : questionsList){
            if (ids.contains(String.valueOf(q.getNumber()))){
                q.setState("Contestada");
            }
        }
        db.close();
        return ids;
    }

    /* recupera una respuesta */
    private JSONObject loadResponseFromDb(int idQuestion) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the primary key value of the new row
        Cursor cursor = db.query(ResponseContract.ResponseEntry.TABLE_NAME,
            new String[]{
                ResponseContract.ResponseEntry.COLUMN_NAME_CONTENT
            }, ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + " = ? AND " +
                ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID+ " = ? AND " +
                ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID+ " = ?",
            new String[]{
                personId,
                String.valueOf(idQuestion),
                poll.getId()
            }, null, null, null);

        JSONObject obj = null;
        if(cursor.moveToFirst()){
            try {
                obj = new JSONObject(cursor.getString(0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        db.close();
        return obj;
    }


    /* Elimina la o las respuestas de un pregunta*/
    private void removeResponseFromDb(int idQuestion) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the primary key value of the new row
        db.delete(ResponseContract.ResponseEntry.TABLE_NAME,
                 ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + " = ? AND " +
                        ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID+ " = ? AND " +
                        ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID+ " = ?",
                new String[]{
                        personId,
                        String.valueOf(idQuestion),
                        poll.getId()
                });

        db.close();
    }

    /* almacena la respuesta a una pregunta. */
    private void saveResponseToDb(int idQuestion, JSONObject content) {
        // se elimina la respuesta previa
        removeResponseFromDb(idQuestion);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.delete(ResponseContract.ResponseEntry.TABLE_NAME,
                ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + "= ? AND " +
                        ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID + " = ?"
                , new String[]{personId, String.valueOf(idQuestion)});

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_CONTENT, content.toString());
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID, poll.getId());
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID, personId);
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID, String.valueOf(idQuestion));

        // se guarda la nueva respuesta
        long newRowId = db.insert(ResponseContract.ResponseEntry.TABLE_NAME, null, values);
        db.close();
    }

    /* si no se completa la encuesta, se eliminan todas las respuestas de la persona. */
    private void removeResponsesFromDb() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(PersonContract.PersonEntry.TABLE_NAME, PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID + "= ? ", new String[]{personId});
        db.delete(ResponseContract.ResponseEntry.TABLE_NAME, ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + "= ? ", new String[]{personId});
        db.close();
    }


    private Question findQuestionByNumber(int n) {
        for (Question q : questionsList) {
            if (q.getNumber() == n) {
                return q;

            }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationListener != null && !workWithoutLocation){
            updatePosition();
        }
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        mLocationManager.removeUpdates(mLocationListener);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(mLocationListener);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!workWithoutLocation){
            updatePosition();
        }

    }

    public class QuestionAdapter extends ArrayAdapter<Question> {
        private Context mContext;
        private List<Question> questionList = new ArrayList<>();

        public QuestionAdapter(@NonNull Context context, ArrayList<Question> list) {
            super(context, 0 , list);
            mContext = context;
            questionList = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_question, parent, false);
            }

            final Question currentQuestion = questionList.get(position);

            currentQuestion.setNumber(position);
            TextView number = (TextView) listItem.findViewById(R.id.question_number);
            number.setText(String.valueOf(position + 1));

            TextView titleView = (TextView) listItem.findViewById(R.id.question_title);
            titleView.setText(currentQuestion.getTitle());
            if (!currentQuestion.isRequired()){
                titleView.setTextColor(getResources().getColor(R.color.colorSecondary));
            }

            AppCompatImageView stateView = (AppCompatImageView) listItem.findViewById(R.id.question_state);
            if (currentQuestion.getState() != "Contestada"){
                stateView.setImageResource(R.drawable.ic_check_box_outline_blank_black_24dp);
            }
            else{
                stateView.setImageResource(R.drawable.ic_check_box_black_24dp);
            }

            return listItem;
        }
    }
}
