package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
    static final public HashMap QUESTION_TYPE_LIST = new HashMap<String, java.lang.Class>(){{
        put("polygon", PolygonActivity.class);
        put("route", RouteActivity.class);
        put("text", TextActivity.class);
        put("choice", MultipleChoiceActivity.class);
        put("point", PointActivity.class);
        put("point+", PointPlusActivity.class);
    }};


    TableLayout mTableQuestions;
    ArrayList<Question> questionsList;
    ResponseDbHelper mDbHelper;
    int pollId;
    int pollCount;

    String pollJson = "[{\"n\":1, \"title\":\"Dibujar poligono\", \"description\":\"Esta es una descripcion\", \"type\":\"polygon\"}," +
            "{\"n\":2, \"required\":false, \"title\":\"Dibujar ruta\", \"description\":\"Esta es una descripcion\", \"type\":\"route\"}," +
            "{\"n\":3, \"title\":\"Ingresar texto\", \"description\":\"Esta es una descripcion\", \"type\":\"text\"}," +
            "{\"n\":4, \"title\":\"Selección multiple\", \"description\":\"Esta es una descripcion\", \"type\":\"choice\", \"alternatives\": [{\"value\": \"1\", \"label\":\"Opcion 1\"}, {\"value\": \"2\", \"label\":\"Opcion 2\"}, {\"value\": \"3\", \"label\":\"Opcion 3\"}, {\"value\": \"4\", \"label\":\"Opcion 4\"}], \"max\":3}, " +
            "{\"n\":5, \"title\":\"Selección unica\", \"description\":\"Esta es una descripcion\", \"type\":\"choice\", \"alternatives\": [{\"value\": \"1\", \"label\":\"Opcion 1\"}, {\"value\": \"2\", \"label\":\"Opcion 2\"}, {\"value\": \"3\", \"label\":\"Opcion 3\"}, {\"value\": \"4\", \"label\":\"Opcion 4\"}]}," +
            "{\"n\":6, \"title\":\"Ingresar un punto\", \"description\":\"Ingrese un punto\", \"type\":\"point\" }," +
            "{\"n\":7, \"title\":\"Ingresar un punto con pregunta\", \"description\":\"Seleccione un punto\", \"type\":\"point+\", " +
            "\"points\" : [{ \"latitude\":-33.4453563917065, \"longitude\":-70.64786536487553}, { \"latitude\":-33.4040745047663, \"longitude\":-70.65084457397461}], " +
            "\"question\" : {\"title\":\"Pregunta secundaria\", \"description\":\"Descripcion secundaria\", \"type\":\"text\"}}" +
            "]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        pollCount = bundle.getInt("POLL_COUNT");
        pollId = Integer.valueOf(bundle.getString("POLL_ID"));

        mTableQuestions = findViewById(R.id.tablequestions);
        questionsList = new ArrayList<Question>();
        try {
            JSONArray jsonArray = new JSONArray(pollJson);
            for (int i=0; i<jsonArray.length(); i++){
                questionsList.add(new Question(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateTable();
        mDbHelper = new ResponseDbHelper(getApplicationContext());// Gets the data repository in write mode
    }


    public void handleRowClick(View view){
        TableRow row = (TableRow) view;
        int number = Integer.valueOf((String) ((TextView)row.getChildAt(0)).getText());
        Log.d("Test mob: ", "openQuestion " + String.valueOf(number));
        Question q = findQuestionByNumber(number);
        if (q != null){
            openQuestion(q);
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
                updateTable();
                saveResponseToDb(q.getNumber(), returnedResult);
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



    public void updateTable(){
        mTableQuestions.removeAllViews();
        for (Question q: questionsList){
            TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.polltable_row, null);
            ((TextView)row.getChildAt(0)).setText(String.valueOf(q.getNumber()));
            TextView titleView = (TextView)row.getChildAt(1);
            titleView.setText(q.getTitle());
            if (q.isRequired()){
                titleView.setTextColor(getColor(R.color.colorInfo));
            }
            ImageView stateView = (ImageView) row.getChildAt(2);
            //stateView.setText(q.getState());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (q.getState() == "Pendiente"){
                    stateView.setImageDrawable(getDrawable(R.drawable.ic_check_box_outline_blank_black_24dp));
                }
                else{
                    stateView.setImageDrawable(getDrawable(R.drawable.ic_check_box_black_24dp));
                }
            }
            else{
                row.removeViewAt(2);
                TextView textView = new TextView(this);
                textView.setText(q.getState());
                row.addView(textView);
            }

            mTableQuestions.addView(row);
        }
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
}
