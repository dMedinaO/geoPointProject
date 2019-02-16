package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import cl.esanhueza.map_david.models.Choice;
import cl.esanhueza.map_david.models.Question;


public class PollEditorActivity extends AppCompatActivity {
    static final public int NEW_QUESTION_CODE = 1;
    static final public int EDIT_QUESTION_CODE = 1;
    QuestionAdapter mAdapter;
    ListView listView;

    static final public HashMap QUESTION_TYPE_LIST = new HashMap<String, String>(){{
        put("choice", "Alternativa");
        put("polygon", "Poligono");
        put("route", "Ruta");
        put("text", "Texto");
        put("point", "Puntos en mapa");
    }};

    TableLayout mTableQuestions;
    ArrayList<Question> questionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_editor);
        Intent intent = getIntent();
        mTableQuestions = findViewById(R.id.tablequestions);
        questionsList = new ArrayList<Question>();

        QuestionAdapter mAdapter = new QuestionAdapter(this, questionsList);
        listView = findViewById(R.id.question_list);
        listView.setAdapter(mAdapter);
    }

    public void handleRowClick(View view){
        TableRow row = (TableRow) view;
        int number = Integer.valueOf((String) ((TextView)row.getChildAt(0)).getText());
        Log.d("Test mob: ", "openQuestion " + String.valueOf(number));
        Question q = findQuestionByNumber(number);
        Intent intent = new Intent(this, QuestionEditorActivity.class);
        if (q != null){
            intent.putExtra("QUESTION", q.toJson());
        }
        startActivityForResult(intent, 0);
    }

    public void handleNewQuestion(View view){
        Intent intent = new Intent(this, QuestionEditorActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            Log.d("Result", "Pregunta contestada, respuesta: " + returnedResult);

            Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();
            Question q = findQuestionByNumber(requestCode);
            if (q != null){
                if (questionsList.contains(q)){
                    questionsList.set(questionsList.indexOf(q), q);
                }
                else{
                    questionsList.add(q);
                }
            }
        }
        else{
            Log.d("Result", "Pregunta cerrada sin terminar de contestar.");
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Encuesta incompleta")
                .setMessage("¿Esta seguro que desea abandonar esta encuesta? Se perdera el progreso.")
                .setPositiveButton("Abandonar", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
        // Otherwise defer to system default behavior.
        super.onBackPressed();
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
            if(listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_choice_item, parent,false);

            final Question currentQuestion = questionList.get(position);

            TextView number = (TextView) listItem.findViewById(R.id.number);
            number.setText("#" + String.valueOf(position));

            final TextView titleView = (TextView) listItem.findViewById(R.id.value);
            titleView.setText(currentQuestion.getTitle());

            TextView typeView = (TextView) listItem.findViewById(R.id.label);
            typeView.setText(currentQuestion.getType());

            return listItem;
        }
    }
}
