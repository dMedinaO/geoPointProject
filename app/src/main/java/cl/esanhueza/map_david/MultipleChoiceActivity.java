package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (maxSelected < 1){
                Toast.makeText(this, "El número de opciones seleccionables debe ser mayor a 1.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        JSONArray jsonArray = (JSONArray) options.get("alternatives");
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.questions);
        if (maxSelected > 1){
            for (int i=0; i<jsonArray.length(); i++){
                try {
                    JSONObject alt = jsonArray.getJSONObject(i);
                    CheckBox box = new CheckBox(getApplicationContext());
                    box.setText(alt.getString("label"));
                    box.setContentDescription(alt.getString("value"));
                    boxes.add(box);
                    linearLayout.addView(box);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            radioGroup = new RadioGroup(getApplicationContext());
            linearLayout.addView(radioGroup);
            for (int i=0; i<jsonArray.length(); i++){
                try {
                    JSONObject alt = jsonArray.getJSONObject(i);
                    RadioButton btn = new RadioButton(getApplicationContext());
                    btn.setText(alt.getString("label"));
                    btn.setContentDescription(alt.getString("value"));
                    radioGroup.addView(btn);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void saveResponse(View view) {
        Intent intent = new Intent();

        if (maxSelected > 1){
            String values = "";
            for (CheckBox box : boxes) {
                if (box.isChecked()) {
                    values = values + box.getContentDescription() + ",";
                }
            }
            if (values != ""){
                JSONObject response = new JSONObject();
                values = values.substring(0, values.length() - 1);
                values = "[" + values + "]";
                try {
                    response.put("value", values);
                    intent.setData(Uri.parse(response.toString()));
                    setResult(Activity.RESULT_OK, intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                setResult(Activity.RESULT_CANCELED, intent);
            }
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
