package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Space;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MultipleChoiceActivity extends QuestionActivity{
    int maxSelected = 1;
    ArrayList<CheckBox> boxes = new ArrayList();
    RadioGroup radioGroup;

    @Override
    public int getContentViewId() {
        return R.layout.activity_multiplechoice;
    }

    public void clean(View view){
        if (maxSelected > 1){
            for (CheckBox box : boxes){
                box.setChecked(false);
            }
        }
        else{
            radioGroup.clearCheck();
        }
    }

    @Override
    public void setContent() {
        Map<String, Object> options = question.getOptions();
        if (!options. containsKey("alternatives")){
            Toast.makeText(this, "Faltan las alternativas para esta pregunta.", Toast.LENGTH_LONG).show();
            return;
        }
        // por defecto solo es posible seleccionar una opcion
        if (options.containsKey("max")){
            maxSelected = Integer.valueOf(String.valueOf(options.get("max")));
        }
        else{
            maxSelected = 100000;
        }
        JSONArray jsonArray = (JSONArray) options.get("alternatives");
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.questions);
        if (maxSelected > 1){
            try {
                for (int i=0; i<jsonArray.length(); i++){
                    ArrayList selectedList = new ArrayList();
                    if (response != null){
                        JSONArray selected = response.getJSONArray("value");
                        for (int j=0; j<selected.length();j++){
                            selectedList.add(selected.get(j));
                        }
                    }
                    JSONObject alt = jsonArray.getJSONObject(i);
                    CheckBox box = new CheckBox(getApplicationContext());
                    box.setText(alt.getString("label"));
                    box.setTextColor(Color.BLACK);
                    box.setContentDescription(alt.getString("value"));

                    box.setChecked(selectedList.contains(alt.getString("value")));
                    boxes.add(box);
                    Space space = new Space(this);
                    space.setMinimumHeight(25);
                    linearLayout.addView(space);
                    linearLayout.addView(box);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            radioGroup = new RadioGroup(getApplicationContext());
            linearLayout.addView(radioGroup);
            for (int i=0; i<jsonArray.length(); i++){
                try {
                    String selected = null;
                    if (response != null){
                        selected = response.getString("value");
                    }
                    JSONObject alt = jsonArray.getJSONObject(i);
                    RadioButton btn = new RadioButton(getApplicationContext());
                    btn.setId(View.generateViewId());
                    btn.setText(alt.getString("label"));
                    btn.setContentDescription(alt.getString("value"));

                    btn.setChecked(alt.getString("value").equals(selected));
                    radioGroup.addView(btn);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean validate(){
        String error = "";
        if (maxSelected > 1){
            int checkedCount = 0;
            for (CheckBox box : boxes) {
                if (box.isChecked()) {
                    checkedCount++;
                }
            }
            if(checkedCount == 0){
                error = getString(R.string.text_question_choice_at_least_one_alternative);
            }
            if (checkedCount > maxSelected){
                error = getString(R.string.text_question_choice_max_alternatives_selected) + String.valueOf(maxSelected) + " ";
            }
        }
        else{
            int idSelected = radioGroup.getCheckedRadioButtonId();
            if (idSelected == -1){
                error = getString(R.string.text_question_choice_at_least_one_alternative);
            }

        }

        if (!error.equals("")){
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void saveResponse(View view) {
        if (!validate()){ return; }
        Intent intent = new Intent();

        if (maxSelected > 1){
            JSONArray arrayResponses = new JSONArray();
            for (CheckBox box : boxes) {
                if (box.isChecked()) {
                    arrayResponses.put(box.getContentDescription());
                }
            }
            JSONObject response = new JSONObject();
            try {
                response.put("value", arrayResponses);
                intent.setData(Uri.parse(response.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setResult(Activity.RESULT_OK, intent);
        }
        else{
            int idSelected = radioGroup.getCheckedRadioButtonId();
            if (idSelected == -1){
                setResult(Activity.RESULT_CANCELED, intent);
            }
            else{
                JSONObject response = new JSONObject();
                String value = (String) ((RadioButton) findViewById(idSelected)).getContentDescription();
                try {
                    response.put("value", value);
                    intent.setData(Uri.parse(response.toString()));
                    setResult(Activity.RESULT_OK, intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        finish();
    }
}
