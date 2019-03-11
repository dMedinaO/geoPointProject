package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class RangeActivity extends QuestionActivity{
    private int min = 0;
    private int max = 100;
    private int currentValue = 1;

    @Override
    public int getContentViewId() {
        return R.layout.activity_range;
    }

    @Override
    public void setContent() {
        if (question.getOptions().containsKey("min")){
            min = Integer.valueOf(question.getOptions().get("min").toString());
        }
        if (question.getOptions().containsKey("max")){
            max = Integer.valueOf(question.getOptions().get("max").toString());
        }
        TextView value = findViewById(R.id.text_value);
        currentValue = min;
        if(response != null){
            try {
                currentValue = response.getInt("value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        value.setText(String.valueOf(currentValue));

        if(response != null){
            try {
                value.setText(String.valueOf(response.getInt("value")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void plus(View view){
        TextView value = findViewById(R.id.text_value);
        if (currentValue + 1 <= max){
            currentValue++;
            value.setText(String.valueOf(currentValue));
        }
    }

    public void minus(View view){
        TextView value = findViewById(R.id.text_value);
        if (currentValue - 1 >= min){
            currentValue--;
            value.setText(String.valueOf(currentValue));
        }
    }

    @Override
    public void saveResponse(View view) {
        Intent intent = new Intent();
        JSONObject response = new JSONObject();
        try {
            response.put("value", currentValue);
            intent.setData(Uri.parse(response.toString()));
            setResult(Activity.RESULT_OK, intent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        };
    }
}
