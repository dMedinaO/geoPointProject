package cl.esanhueza.map_david;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionEditorFragment extends Fragment {
    Map<String, Object> options;

    public Map<String, Object> getOptions(){
        return null;
    }
    public boolean validate(){
        return true;
    }
    public QuestionEditorFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
    }


    public void setOptions(Map<String, Object> options){
        this.options = options;
    }
}
