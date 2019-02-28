package cl.esanhueza.map_david.storage;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;

import cl.esanhueza.map_david.models.Poll;

public class PollFileStorageHelper {
    static final String LOG_TAG = "TEST ENCUESTAS: ";
    static final String FOLDER_NAME = "ENCUESTAS";
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getPublicPollStorageDir() {
        // Get the directory for the user's public pictures directory.
        Log.d("TST ENCUESTAS: ", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + File.separator + FOLDER_NAME);

        if (!folder.mkdirs()) {
            Log.i(LOG_TAG, "Directory not created");
        }
        return folder;
    }

    static final public String savePoll(Poll poll){
        try {
            JSONObject json = new JSONObject(poll.toJson());
            File folder = PollFileStorageHelper.getPublicPollStorageDir();
            String title = poll.getTitle();
            title.replaceAll(" ", "_");
            File file = new File(folder.getAbsolutePath(),  title + "_" + poll.getId() + ".json");
            if (!file.exists()){
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.append(json.toString());
                writer.close();

            }
            else{
                Log.e(LOG_TAG, "Archivo json ya existe.");
            }

            return file.getAbsolutePath();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static final public ArrayList<Poll> readPolls(){
        ArrayList<Poll> polls = new ArrayList<>();
        try {
            File folder = PollFileStorageHelper.getPublicPollStorageDir();

            File[] files = folder.listFiles();
            for (int i=0; i<files.length; i++){
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                BufferedReader br = new BufferedReader(new FileReader(files[i]));

                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line);
                }

                JSONObject json = new JSONObject(stringBuilder.toString());
                polls.add(new Poll(json));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return polls;
    }
}
