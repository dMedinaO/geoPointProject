package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cl.esanhueza.map_david.models.Question;

public class QuestionEditorActivity extends AppCompatActivity{
    final static int PICKFILE_REQUEST_CODE = 9999;
    Question question;
    ArrayList<String> types = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();
    private String uriImage;
    ImageView imageView;

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
            Map.Entry map = (Map.Entry<String, Integer>) entry;
            types.add(getString((Integer) map.getValue()));
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
                LinearLayout linearLayout = findViewById(R.id.question_details);
                linearLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                LinearLayout linearLayout = findViewById(R.id.question_details);
                linearLayout.setVisibility(View.GONE);
            }
        });

        ImageButton btn = findViewById(R.id.btn_attach_image);

        imageView = findViewById(R.id.image_attached);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachImage();
            }
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
                currentFragment = new RouteEditorFragment();
                break;
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

    private void attachImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                uriImage = data.getData().toString();
                imageView.setImageURI(Uri.parse(uriImage));
                imageView.setVisibility(View.VISIBLE);
            }
        }
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
        // no se agrega la imagen directamente, se genera un archivo en el dispositivo y se pasa
        // la direccion.
        if (uriImage != null){
            String fileName = UUID.randomUUID().toString();
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(encodeImage(uriImage).getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            question.putOption("image", true);
            question.putOption("imagePath", fileName);
            //question.putOption("image", encodeImage(uriImage));
        }

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

        if(uriImage != null){
            imageView.setImageURI(Uri.parse(uriImage));
        }
        else{
            if (question.getOptions().containsKey("image")){
                imageView = findViewById(R.id.image_attached);
                try {
                    StringBuffer fileContent = new StringBuffer("");
                    byte[] buffer = new byte[1024];
                    FileInputStream fileInputStream = openFileInput(question.getOptions().get("imagePath").toString());
                    int n;
                    while ((n = fileInputStream.read(buffer)) != -1)
                    {
                        fileContent.append(new String(buffer, 0, n));
                    }
                    byte[] decodedImageBytes = Base64.decode(fileContent.toString(), Base64.DEFAULT);
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.length));
                    imageView.setVisibility(View.VISIBLE);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
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

    private String encodeImage(String uriString){
        InputStream inputStream = null;//You can get an inputStream using any IO API
        try {
            inputStream = new FileInputStream(getApplicationContext().getContentResolver().openFileDescriptor(Uri.parse(uriString), "r").getFileDescriptor());
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
}
