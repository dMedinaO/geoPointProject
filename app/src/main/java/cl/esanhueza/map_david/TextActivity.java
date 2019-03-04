package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.List;

public class TextActivity extends QuestionActivity{
    @Override
    public int getContentViewId() {
        return R.layout.activity_text;
    }

    public void clean(View view){
        EditText edit = (EditText) findViewById(R.id.editText);
        edit.getEditableText().clear();
    }

    @Override
    public void setContent() {

    }

    @Override
    public void saveResponse(View view) {
        EditText edit = (EditText) findViewById(R.id.editText);
        Intent intent = new Intent();
        String text = edit.getText().toString();
        JSONObject response = new JSONObject();
        try {
            response.put("value", text);
            intent.setData(Uri.parse(response.toString()));
            setResult(Activity.RESULT_OK, intent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        };
    }
}
