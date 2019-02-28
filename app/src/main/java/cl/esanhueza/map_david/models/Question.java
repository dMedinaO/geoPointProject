package cl.esanhueza.map_david.models;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import cl.esanhueza.map_david.PollEditorActivity;

public class Question {
    static final String[] BASIC_OPTIONS = {"n", "title", "description", "type", "required"};
    private int number;
    private String title;
    private String type;
    private String description;
    private String state;
    private boolean required;
    private Map<String, Object> options = new HashMap<String, Object>();

    public Question(JSONObject obj) {
        try {
            this.number = obj.getInt("n");
            this.title = obj.optString("title");
            this.type = obj.getString("type");
            this.description = obj.optString("description");
            this.required = obj.optBoolean("required", true);
            this.state = "Pendiente";
            for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                if (!Arrays.asList(BASIC_OPTIONS).contains(key)) {
                    this.options.put(key, obj.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static final public Question generate(int n){
        Question q = new Question();
        q.setNumber(n);
        q.setTitle("Pregunta " + String.valueOf(n));
        q.setDescription("Descripcion");

        Random rand = new Random();
        int nType = rand.nextInt(PollEditorActivity.QUESTION_TYPE_LIST.size());
        String keyType = (String) PollEditorActivity.QUESTION_TYPE_LIST.keySet().toArray()[nType];
        q.setType(keyType);

        // si se genera una opcion de tipo "choice", se crean las alternativas tambien
        if (keyType == "choice"){
            q.getOptions().put("max", 2);
            JSONArray alt = new JSONArray();
            for (int i=0; i<4; i++){
                JSONObject obj = new JSONObject();
                try {
                    obj.put("label", "Opcion " + String.valueOf(i) );
                    obj.put("value", i );
                    alt.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            q.getOptions().put("alternatives", alt);
        }
        q.setRequired(true);
        return q;
    }

    public Question(HashMap obj) {
        this.number = (int) obj.get("n");
        this.title = (String) obj.get("title");
        this.type = (String) obj.get("type");
        this.description = (String) obj.get("description");
        this.required = true;
        this.state = "Pendiente";
    }

    public Question() {

    }

    public void putOption(String key, Object obj) {
        this.options.put(key, obj);
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String toJson(){
        ArrayList<String> basicOptions = new ArrayList<String>();
        basicOptions.add("n");
        basicOptions.add("title");
        basicOptions.add("type");
        basicOptions.add("description");
        basicOptions.add("required");
        JSONObject obj = new JSONObject();
        try {
            obj.put("n", this.number);
            obj.put("title", this.title);
            obj.put("description", this.description);
            obj.put("type", this.type);
            obj.put("required", this.required);
            obj.put("state", this.state);;
            for (String key : options.keySet()){
                obj.put(key, options.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    public boolean isRequired() {
        return required;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        if (this.title == null){
            return "No definido";
        }
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        if (this.description == null){
            return "No definido";
        }
        return description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


}
