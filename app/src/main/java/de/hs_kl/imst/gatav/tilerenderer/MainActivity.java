package de.hs_kl.imst.gatav.tilerenderer;

import android.content.Intent;
import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AssetManager am = getResources().getAssets();
        ArrayList<String> levelList = new ArrayList<String>();  // alle Level-Namen ohne .txt

        try {
            String[] files = am.list("levels");
            for(String s : files) {
                if(!s.endsWith(".txt")) continue;
                s = s.substring(0, s.lastIndexOf("."));
                levelList.add(s);
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, R.layout.row, levelList); // besitze ArrayAdapter mit String

        ListView listView = (ListView) findViewById(R.id.level_list); //
        listView.setAdapter(itemsAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
			    //Toast.makeText(getApplicationContext(), ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
                String level = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, MainGameActivity.class);
                intent.putExtra("level", level);
                startActivity(intent);
			}
		});
    }


}

