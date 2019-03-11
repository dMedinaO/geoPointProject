package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RangeEditorFragment extends QuestionEditorFragment {

    @Override
    public boolean validate(){
        return true;
    }

    public Map<String, Object> getOptions(){
        Map<String, Object> map = new HashMap<String, Object>();
        TextView min = getView().findViewById(R.id.text_min);
        TextView max = getView().findViewById(R.id.text_max);
        map.put("min", min.getText().toString());
        map.put("max", max.getText().toString());
        return map;
    }

    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        if (options.containsKey("min")){
            TextView min = view.findViewById(R.id.text_min);
            min.setText(options.get("min").toString());
        }
        if (options.containsKey("max")){
            TextView max = view.findViewById(R.id.text_max);
            max.setText(options.get("max").toString());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view =  inflater.inflate(R.layout.fragment_range, container, false);
        ImageButton btnMinusMin = view.findViewById(R.id.btn_minus_min);
        btnMinusMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                int value = Integer.valueOf(min.getText().toString()) - 1;
                min.setText(String.valueOf(value));
            }
        });
        ImageButton btnPlusMin = view.findViewById(R.id.btn_plus_min);
        btnPlusMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                TextView max = view.findViewById(R.id.text_max);
                int value = Integer.valueOf(min.getText().toString());
                int maxValue = Integer.valueOf(max.getText().toString());
                if (value + 1 < maxValue){
                    min.setText(String.valueOf(value + 1));
                }
            }
        });

        ImageButton btnMinuxMax = view.findViewById(R.id.btn_minus_max);
        btnMinuxMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                TextView max = view.findViewById(R.id.text_max);
                int minValue = Integer.valueOf(min.getText().toString());
                int maxValue = Integer.valueOf(max.getText().toString());
                if (maxValue - 1 > minValue){
                    max.setText(String.valueOf(maxValue - 1));
                }
            }
        });
        ImageButton btnPlusMax = view.findViewById(R.id.btn_plus_max);
        btnPlusMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView max = view.findViewById(R.id.text_max);
                int value = Integer.valueOf(max.getText().toString()) + 1;
                max.setText(String.valueOf(value));
            }
        });
        updateQuestionContent(view);
        return view;
    }
}
