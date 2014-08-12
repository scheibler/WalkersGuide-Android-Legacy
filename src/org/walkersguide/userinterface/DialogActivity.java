package org.walkersguide.userinterface;

import org.walkersguide.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DialogActivity extends  Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        Intent sender = getIntent();
        String message = sender.getExtras().getString("message", "");
        TextView labelMessage = (TextView) findViewById(R.id.labelMessage);
        labelMessage.setText(message);
        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent resultData = new Intent();
                setResult(RESULT_OK, resultData);
                finish();
            }
        });
    }
}
