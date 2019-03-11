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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextEditorFragment extends QuestionEditorFragment {
    final static int PICKFILE_REQUEST_CODE = 9999;
    private String uriImage;
    @Override
    public boolean validate(){
        return true;
    }

    private String encodeImage(String uriString){
        InputStream inputStream = null;//You can get an inputStream using any IO API
        try {
            inputStream = new FileInputStream(getContext().getContentResolver().openFileDescriptor(Uri.parse(uriString), "r").getFileDescriptor());
            byte[] bytes;
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bytes = output.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getOptions(){
        Map<String, Object> map = new HashMap<String, Object>();
        if (uriImage != null){
            map.put("image", encodeImage(uriImage));
        }
        else if (options.containsKey("image")){
            map.put("image", options.get("image"));
        }
        return map;
    }

    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        if (options.containsKey("image")){
            ImageView imageView = view.findViewById(R.id.image_attached);
            byte[] decodedImageBytes = Base64.decode(options.get("image").toString(), Base64.DEFAULT);
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.length));
            imageView.setVisibility(View.VISIBLE);
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
        View view =  inflater.inflate(R.layout.fragment_text, container, false);
        ImageButton btn = view.findViewById(R.id.btn_attach_image);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachImage();
            }
        });
        updateQuestionContent(view);
        return view;
    }

    private void attachImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                TextView textView = getView().findViewById(R.id.text_image_attached);
                uriImage = data.getData().toString();
                textView.setText(uriImage);
                ImageView imageView = getView().findViewById(R.id.image_attached);
                imageView.setImageURI(Uri.parse(uriImage));
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }
}
