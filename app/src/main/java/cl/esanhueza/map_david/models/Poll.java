package cl.esanhueza.map_david.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Poll {
    private String id;
    private String title;
    private String description;
    private ArrayList<Question> questions = new ArrayList<>();

    public Poll(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public Poll() {

    }

    public Poll(JSONObject obj){
        try {
            this.id = obj.getString("id");
            this.title = obj.getString("title");
            this.description = obj.getString("description");
            JSONArray array = obj.getJSONArray("questions");
            for (int i=0; i<array.length(); i++){
                questions.add(new Question(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static final Poll generate(String id){
        Poll poll = new Poll(id, "Encuesta ", "Descripcion " + id);
        for (int i=0; i<15; i++){
            poll.addQuestion(Question.generate(i+1));
        }
        return poll;
    }

    public String toJson(){
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("title", this.title);
            json.put("description", this.description);
            JSONArray array = new JSONArray();
            for (Question q : questions){
                array.put(new JSONObject(q.toJson()));
            }
            json.put("questions", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(ArrayList<Question> questions) {
        this.questions = questions;
    }

    public void addQuestion(Question q) {
        this.questions.add(q);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
