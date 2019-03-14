package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cl.esanhueza.map_david.models.Choice;
import cl.esanhueza.map_david.models.Item;

    public class RangeActivity extends QuestionActivity{
        private int min = 0;
        private int max = 100;
        private int currentValue = 1;
        private cl.esanhueza.map_david.RangeActivity.ItemAdapter mAdapter;
        private ListView listView;

        @Override
        public int getContentViewId() {
            return R.layout.activity_range;
        }

        @Override
        public void setContent() {
            mAdapter = new cl.esanhueza.map_david.RangeActivity.ItemAdapter(this, new ArrayList<Item>());
            listView = findViewById(R.id.item_list);
            listView.setAdapter(mAdapter);

            if (question.getOptions().containsKey("min")){
                min = Integer.valueOf(question.getOptions().get("min").toString());
            }
            if (question.getOptions().containsKey("max")){
                max = Integer.valueOf(question.getOptions().get("max").toString());
            }

            if (question.getOptions().containsKey("items")){
                JSONArray jsonArray = (JSONArray) question.getOptions().get("items");
                ArrayList<Item> items = new ArrayList<>();
                for (int i=0; i<jsonArray.length(); i++){
                    try {
                        Item item = new Item(jsonArray.getJSONObject(i));
                        item.setValue(min);
                        items.add(item);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                mAdapter.addAll(items);
            }

            if(response != null){
                try {
                    JSONArray jsonArray = response.getJSONArray("value");
                    for (int i=0; i<jsonArray.length(); i++){
                        try {
                            mAdapter.getItem(i).setValue(jsonArray.getInt(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            setListViewHeightBasedOnChildren(listView);
        }

        @Override
        public void saveResponse(View view) {
            mAdapter.saveEdit();
            Intent intent = new Intent();
            JSONObject response = new JSONObject();
            try {
                JSONArray jsonArray = new JSONArray();
                for (int i=0; i<mAdapter.getCount(); i++){
                    jsonArray.put(mAdapter.getItem(i).getValue());
                }
                response.put("value", jsonArray);
                intent.setData(Uri.parse(response.toString()));
                setResult(Activity.RESULT_OK, intent);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            };
        }

        public static void setListViewHeightBasedOnChildren(ListView listView) {
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter == null) {
                // pre-condition
                return;
            }

            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
            listView.setLayoutParams(params);
            listView.requestLayout();
        }

        public void plus(View view){
            Item item = mAdapter.getItem(Integer.parseInt(view.getContentDescription().toString()));
            if (item.getValue() + 1 <= max){
                item.setValue(item.getValue() + 1);
            }
            mAdapter.notifyDataSetChanged();
        }

        public void minus(View view){
            Item item = mAdapter.getItem(Integer.parseInt(view.getContentDescription().toString()));
            if (item.getValue() - 1 >= min){
                item.setValue(item.getValue() - 1);
            }
            mAdapter.notifyDataSetChanged();
        }

        public class ItemAdapter extends ArrayAdapter<Item> {
            private Context mContext;

            public ItemAdapter(@NonNull Context context, ArrayList<Item> list) {
                super(context, 0 , list);
                mContext = context;
            }

            public void saveEdit(){
                for (int i=0; i<this.getCount(); i++){
                    TextView label = listView.findViewWithTag("label" + String.valueOf(i));
                    if(label != null){
                        this.getItem(i).setLabel(label.getText().toString());
                    }

                    TextView value = listView.findViewWithTag("value" + String.valueOf(i));
                    if(value != null){
                        this.getItem(i).setValue(Integer.parseInt(value.getText().toString()));
                    }
                }
            }

            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View listItem = convertView;
                if(listItem == null)
                    listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_rangeitem, parent,false);

                final Item currentChoice = getItem(position);

                final TextView labelView = (TextView) listItem.findViewById(R.id.text);
                labelView.setTag("label" + String.valueOf(position));
                labelView.setText(currentChoice.getLabel());

                final TextView valueView = (TextView) listItem.findViewById(R.id.value);
                valueView.setTag("value" + String.valueOf(position));
                if(currentChoice.getValue() != -9999){
                    valueView.setText(String.valueOf(currentChoice.getValue()));
                }
                else{
                    valueView.setText(String.valueOf(min));
                }


                final ImageButton btnMinus = (ImageButton) listItem.findViewById(R.id.btn_minus);
                btnMinus.setContentDescription(String.valueOf(position));
                btnMinus.setOnClickListener(new ImageButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        minus(v);
                    }
                });

                final ImageButton btnMax = (ImageButton) listItem.findViewById(R.id.btn_plus);
                btnMax.setContentDescription(String.valueOf(position));
                btnMax.setOnClickListener(new ImageButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        plus(v);
                    }
                });

                return listItem;
            }
        }
    }