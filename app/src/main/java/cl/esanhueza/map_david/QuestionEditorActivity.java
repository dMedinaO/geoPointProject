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

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cl.esanhueza.map_david.models.Question;

public class QuestionEditorActivity extends AppCompatActivity implements BlankFragment.OnFragmentInteractionListener, ChoicesEditorFragment.OnFragmentInteractionListener{
    Question question;
    ArrayList<String> types = new ArrayList<>();
    String[] keys;
    Fragment currentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_question_editor);

        Intent intent = getIntent();
        if (intent.hasExtra("QUESTION")){
            String message = intent.getStringExtra("QUESTION");
            try {
                question = new Question(new JSONObject(message));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
             question = new Question();
        }
        
        Set keysSet = PollEditorActivity.QUESTION_TYPE_LIST.keySet();
        Set entriesSet = PollEditorActivity.QUESTION_TYPE_LIST.entrySet();
        keys = (String[]) keysSet.toArray(new String[keysSet.size()]);
        Object[] entries = entriesSet.toArray();
        for (Object entry : entries){
            Map.Entry map = (Map.Entry<String,String>) entry;
            types.add((String) map.getValue());
        }

        Spinner typeSpinner = (Spinner) findViewById(R.id.edittype);
        typeSpinner.setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.spinner_question_type_item,
                types
        ));

        typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                setContent(keys[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });
    }

    public void setContent(String type){
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
            default:
                currentFragment = new BlankFragment();
                break;
        }

        fragmentTransaction.add(R.id.fragmentContainer, currentFragment);
        fragmentTransaction.commit();
    }

    /* se ejecuta al presionar el boton flotante en el fragmento para editar la pregunta */
    public void saveQuestion(View view) {
        ChoicesEditorFragment f = (ChoicesEditorFragment) currentFragment;
        Map<String, String> editedMap = f.getQuestion();

        for (String key: editedMap.keySet()){
            question.putOption(key, editedMap.get(key));
        }

        Log.d("TEST: ", question.toJson());

        Intent intent = new Intent();
        intent.setData(Uri.parse(question.toJson()));
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
