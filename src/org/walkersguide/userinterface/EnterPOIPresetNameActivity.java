package org.walkersguide.userinterface;

import org.walkersguide.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EnterPOIPresetNameActivity extends Activity {

    private EditText editPresetName;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_enter_poi_preset_name);

        editPresetName = (EditText) findViewById(R.id.editPresetName);
        editPresetName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String name = editPresetName.getText().toString();
                    if (name.equals("")) {
                        Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.messageNoPOIPresetName),
                            Toast.LENGTH_LONG).show();
                        return false;
                    }
                    Intent intent = new Intent();
                    intent.putExtra("presetName", editPresetName.getText().toString() );
                    setResult(RESULT_OK, intent);
                    finish();
                }
                return false;
            }
        });

        Button buttonDelete = (Button) findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                editPresetName.setText("");
                editPresetName.postDelayed(new Runnable() {
                    public void run() {
                        editPresetName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editPresetName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                     }
                }, 200);
            }
        });

        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String name = editPresetName.getText().toString();
                if (name.equals("")) {
                    Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.messageNoPOIPresetName),
                        Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("presetName", editPresetName.getText().toString() );
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
	}
}
