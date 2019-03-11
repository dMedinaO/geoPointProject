package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import cl.esanhueza.map_david.models.Question;

public class QuestionEditorActivity extends AppCompatActivity{
    Question question;
    ArrayList<String> types = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();

    boolean updatingQuestion = false;
    QuestionEditorFragment currentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_question_editor);
        setTitle("Editor de pregunta");

        Intent intent = getIntent();
        if (intent.hasExtra("QUESTION")){
            String message = intent.getStringExtra("QUESTION");
            try {
                question = new Question(new JSONObject(message));
                setQuestionData(question);
                updatingQuestion = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
             question = new Question();
             question.setNumber(-1);
        }

        final Set keysSet = PollEditorActivity.QUESTION_TYPE_LIST.keySet();
        Set entriesSet = PollEditorActivity.QUESTION_TYPE_LIST.entrySet();
        keys = new ArrayList<String>(keysSet);

        Object[] entries = entriesSet.toArray();

        for (Object entry : entries){
            Map.Entry map = (Map.Entry<String,String>) entry;
            types.add((String) map.getValue());
        }

        Spinner typeSpinner = (Spinner) findViewById(R.id.spinner_question_type);
        typeSpinner.setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.spinner_question_type_item,
                types
        ));

        typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                setFragment(keys.get(position));
                question.setType(keys.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });
    }

    // cambia el fragmento del formulario, dependiendo del tipo de pregunta
    public void setFragment(String type){
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (!fragmentManager.getFragments().isEmpty()) {
            for (Fragment f : fragmentManager.getFragments()) {
                fragmentTransaction.remove(f);
            }
        }
        switch (type){
            case "choice":
                currentFragment = new ChoicesEditorFragment();
                break;
            case "text":
                currentFragment = new TextEditorFragment();
                break;
            case "range":
                currentFragment = new RangeEditorFragment();
                break;
            case "route":
            case "polygon":
            case "point":
                currentFragment = new RouteEditorFragment();
                break;
            case "point+":
                currentFragment = new PointPlusEditorFragment();
                break;
            default:
                currentFragment = new BlankFragment();
                break;
        }

        currentFragment.setOptions(question.getOptions());
        fragmentTransaction.add(R.id.fragmentContainer, (Fragment) currentFragment);
        fragmentTransaction.commit();
    }

    /* se ejecuta al presionar el boton flotante en el fragmento para editar la pregunta */
    public void saveQuestion(View view) {
        QuestionEditorFragment f = (QuestionEditorFragment) currentFragment;

        if (!f.validate()){
            return;
        }
        Map<String, Object> editedMap = f.getOptions();

        question.setDescription(getQuestionDescription().getText().toString());
        question.setTitle(getQuestionTitle().getText().toString());

        if (editedMap != null){
            for (String key: editedMap.keySet()){
                question.putOption(key, editedMap.get(key));
            }
        }

        Log.d("TEST ENCUESTA: ", question.toJson());
        Intent intent = new Intent();
        intent.setData(Uri.parse(question.toJson()));
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    // actualiza el formulario con los valores indicados en el objeto "question" entregado
    public void setQuestionData(Question question){
        TextView titleView = (TextView) findViewById(R.id.textview_question_title);
        titleView.setText(String.valueOf(question.getTitle()));

        TextView descriptionView = (TextView) findViewById(R.id.textview_question_description);
        descriptionView.setText(String.valueOf(question.getDescription()));

        Spinner typeSpinner = (Spinner) findViewById(R.id.spinner_question_type);
        typeSpinner.setSelection(keys.indexOf(question.getType()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (updatingQuestion){
            setQuestionData(question);
        }
    }

    public TextView getQuestionTitle(){
        return (TextView) findViewById(R.id.textview_question_title);
    }

    public TextView getQuestionDescription(){
        return (TextView) findViewById(R.id.textview_question_description);
    }
}
