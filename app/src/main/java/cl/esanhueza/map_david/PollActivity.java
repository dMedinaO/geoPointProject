package cl.esanhueza.map_david;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.esanhueza.map_david.models.Question;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;

import static cl.esanhueza.map_david.R.color.colorSecondary;
import static cl.esanhueza.map_david.R.drawable.ic_check_box_black_24dp;

public class PollActivity extends AppCompatActivity {
    static final public HashMap QUESTION_TYPE_LIST = new HashMap<String, java.lang.Class>() {{
        put("polygon", PolygonActivity.class);
        put("route", RouteActivity.class);
        put("text", TextActivity.class);
        put("choice", MultipleChoiceActivity.class);
        put("point", PointActivity.class);
        put("point+", PointPlusActivity.class);
    }};

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private GeoPoint currentLocation;

    ArrayList<Question> questionsList = new ArrayList<>();
    ResponseDbHelper mDbHelper;
    String pollId;
    int pollCount;

    PollActivity.QuestionAdapter mAdapter;
    ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        pollCount = bundle.getInt("POLL_COUNT");
        pollId = bundle.getString("POLL_ID");
        String poll = bundle.getString("POLL");

        try {
            JSONObject pollJson = new JSONObject(poll);
            setTitle(pollJson.getString("title"));
            TextView viewDescription = findViewById(R.id.polldescription);
            viewDescription.setText(pollJson.getString("description"));
            JSONArray questionArray = pollJson.getJSONArray("questions");
            for (int i=0; i<questionArray.length(); i++){
                questionsList.add(new Question(questionArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mDbHelper = new ResponseDbHelper(getApplicationContext());// Gets the data repository in write mode

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                TextView positionView = findViewById(R.id.position);
                positionView.setText(String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("TEST ENCUESTA: ", "onStatusChanged");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("TEST ENCUESTA: ", "onProviderEnabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("TEST ENCUESTA: ", "onProviderDisabled");
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else {
            Log.d("TEST ENCUESTA: ", "Actualizando posicion.");
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        }

        mAdapter = new PollActivity.QuestionAdapter(this, questionsList);
        listView = findViewById(R.id.question_list);
        listView.setAdapter(mAdapter);

        // al dar click sobre una fila, pasar pregunta al editor de preguntas
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("TST ENCUESTAS: ", String.valueOf(position));
                openQuestion(questionsList.get(position));
            }
        });

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                //mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            }
        }
    }

    public void openQuestion(Question q){
        // se revisa si el tipo de pregunta esta previamente implementado.
        if (!QUESTION_TYPE_LIST.containsKey(q.getType())){
            Toast.makeText(this, "El tipo de pregunta seleccionado no está implementado.", Toast.LENGTH_LONG).show();
            return;
        }
        java.lang.Class activity = (Class) QUESTION_TYPE_LIST.get(q.getType());
        Intent intent = new Intent(this, activity);
        intent.putExtra("QUESTION", q.toJson());
        if (currentLocation != null){
            intent.putExtra("POSITION", "{\"latitude\": " + String.valueOf(currentLocation.getLatitude()) + ", \"longitude\": "+ String.valueOf(currentLocation.getLongitude()) +"}");
        }
        startActivityForResult(intent, q.getNumber());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            Log.d("Result", "Pregunta contestada, respuesta: " + returnedResult);

            Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();
            Question q = findQuestionByNumber(requestCode);
            if (q != null){
                q.setState("Contestada");
                saveResponseToDb(q.getNumber(), returnedResult);
                mAdapter.notifyDataSetChanged();
                checkCompleted();
            }
        }
        else{
            Log.d("Result", "Pregunta cerrada sin terminar de contestar.");
        }
    }

    public void checkCompleted(){
        boolean completed = true;
        for (Question q: questionsList){
            if (q.getState() == "Pendiente" && q.isRequired()){
                completed = false;
            }
        }
        if (completed){
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Encuesta completada")
                    .setMessage("Ahora podra abandonar la encuesta de forma segura.")
                    .setPositiveButton("Salir", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        for (Question q: questionsList){
            if (q.getState() == "Pendiente" && q.isRequired()){
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Encuesta incompleta")
                        .setMessage("¿Esta seguro que desea abandonar esta encuesta? Se perdera el progreso.")
                        .setPositiveButton("Abandonar", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeResponsesFromDb();
                                finish();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return;
            }
        }
        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }


    private void saveResponseToDb(int idQuestion, String content){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_CONTENT, content);
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID, String.valueOf(pollId));
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID, String.valueOf(pollCount));
        values.put(ResponseContract.ResponseEntry.COLUMN_NAME_QUESTION_ID, String.valueOf(idQuestion));

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(ResponseContract.ResponseEntry.TABLE_NAME, null, values);
        db.close();
    }

    private void removeResponsesFromDb(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        // Insert the new row, returning the primary key value of the new row
        db.delete(ResponseContract.ResponseEntry.TABLE_NAME, ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID + "=" + String.valueOf(pollCount), null);
        db.close();
    }


    private Question findQuestionByNumber(int n){
        for (Question q : questionsList){
            if (q.getNumber() == n){
                Log.d("Test mob: ", "questionType " + q.getType());
                return q;

            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
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
