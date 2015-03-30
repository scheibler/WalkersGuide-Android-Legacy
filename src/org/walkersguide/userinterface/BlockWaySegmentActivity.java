package org.walkersguide.userinterface;

import org.walkersguide.R;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.SettingsManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BlockWaySegmentActivity extends AbstractActivity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private FootwaySegment footway;
    private LinearLayout mainLayout;
    private Toast messageToast;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_block_way_segment);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();

        Intent sender=getIntent();
        footway = ObjectParser.parseRouteObject(sender.getExtras().getString("footway_object")).getFootwaySegment();
        if (footway == null) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", getResources().getString(R.string.messageEmptyRoutePoint));
            startActivity(intent);
            finish();
        }

        EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
        editName.setText(footway.getName());
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                    if (editName.getText().toString().equals("")) {
                        messageToast.setText(getResources().getString(R.string.messageNoBlockedWayName));
                        messageToast.show();
                        return false;
                    }
                    footway.addName(editName.getText().toString());
                    settingsManager.blockFootwaySegment(footway);
                    messageToast.setText(getResources().getString(R.string.messageBlockedWayStored));
                    messageToast.show();
                    setResult(RESULT_OK, null);
                    finish();
                }
                return false;
            }
        });

        Button buttonDelete = (Button) mainLayout.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                editName.setText("");
                editName.postDelayed(new Runnable() {
                    public void run() {
                        EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                        editName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                if (editName.getText().toString().equals("")) {
                    messageToast.setText(getResources().getString(R.string.messageNoBlockedWayName));
                    messageToast.show();
                    return;
                }
                footway.addName(editName.getText().toString());
                settingsManager.blockFootwaySegment(footway);
                messageToast.setText(getResources().getString(R.string.messageBlockedWayStored));
                messageToast.show();
                setResult(RESULT_OK, null);
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
