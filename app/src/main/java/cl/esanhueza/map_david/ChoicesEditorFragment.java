package cl.esanhueza.map_david;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.esanhueza.map_david.models.Choice;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChoicesEditorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChoicesEditorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChoicesEditorFragment extends Fragment {
    private ListView listView;
    private ChoiceAdapter mAdapter;
    private ArrayList<Choice> choicesList = new ArrayList<Choice>();
    private OnFragmentInteractionListener mListener;

    public Map<String, String> getQuestion(){
        mAdapter.notifyDataSetChanged();
        Map<String, String> map = new HashMap<String, String>();
        String alternatives = "";

        for (int i=0; i<mAdapter.getCount(); i++){
            Choice choice = mAdapter.getItem(i);
            alternatives += "{\"label\": \"" + choice.getLabel()  + "\", \"value\": \""+ choice.getValue() +"\"},";
        }
        alternatives = alternatives.substring(0, alternatives.length() - 1);
        alternatives = "[" + alternatives + "]";

        TextView maxView = this.getView().findViewById(R.id.max_choices);
        map.put("alternatives", alternatives);
        map.put("max", String.valueOf(maxView.getText()));
        return map;
    }

    public ChoicesEditorFragment() {
        // Required empty public constructor
    }

    public void addChoice(){
        choicesList.add(new Choice("Valor", "Texto"));
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChoicesEditorFragment.
     */

    public static ChoicesEditorFragment newInstance() {
        ChoicesEditorFragment fragment = new ChoicesEditorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ChoiceAdapter(getContext(), choicesList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_choices_list, container, false);

        listView = view.findViewById(R.id.choices_list);
        listView.setAdapter(mAdapter);

        Button btn = view.findViewById(R.id.btn_add_choice);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addChoice();
            }
        });
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class ChoiceAdapter extends ArrayAdapter<Choice> {

        private Context mContext;
        private List<Choice> choicesList = new ArrayList<>();

        public ChoiceAdapter(@NonNull Context context, ArrayList<Choice> list) {
            super(context, 0 , list);
            mContext = context;
            choicesList = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if(listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_choice_item, parent,false);

            final Choice currentChoice = choicesList.get(position);

            TextView number = (TextView) listItem.findViewById(R.id.number);
            number.setText(String.valueOf(position));

            final TextView valueView = (TextView) listItem.findViewById(R.id.value);
            valueView.setText(currentChoice.getValue());
            valueView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentChoice.setValue(String.valueOf(s));
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });


            TextView labelView = (TextView) listItem.findViewById(R.id.label);
            labelView.setText(currentChoice.getLabel());
            labelView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentChoice.setLabel(String.valueOf(s));
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            return listItem;
        }
    }
}


