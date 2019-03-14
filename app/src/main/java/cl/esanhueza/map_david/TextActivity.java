package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

    public boolean validate(){
        String error = "";
        TextView textView = findViewById(R.id.editText);
        if (textView.getText().toString().equals("")){
            error = "Debe ingresar la respuesta.";
        }

        if (!error.equals("")){
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void setContent() {
        if(response != null){
            EditText edit = (EditText) findViewById(R.id.editText);
            try {

                edit.getEditableText().clear();
                edit.getEditableText().append(response.getString("value"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
