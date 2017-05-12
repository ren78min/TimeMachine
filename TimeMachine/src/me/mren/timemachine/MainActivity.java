package me.mren.timemachine;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String CLASS_ID = "MainActivity";
	private static final int ENABLE_BLUETOOTH = 1;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothHelper bluetoothHelper;

	// private static String DEVICE_MAC = "20:13:07:18:10:46";
	private static String DEVICE_NAME = "phoenix";
	// private static String DEVICE_PIN = "1234";

	private EditText triger;
	private EditText release;
	private EditText total;
	private EditText counter;
	private EditText updated;
	private EditText finish;
	private ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(this,
					"Bluetooth is NOT supported\nExiting TimeMachine ...",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (bluetoothAdapter.isEnabled()) {
			startConnection();
		} else {
			Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBt, ENABLE_BLUETOOTH);
		}

		setContentView(R.layout.activity_main);

		triger = (EditText) findViewById(R.id.editTextTriger);
		release = (EditText) findViewById(R.id.editTextRelease);
		total = (EditText) findViewById(R.id.editTextTotal);
		counter = (EditText) findViewById(R.id.editTextCounter);
		updated = (EditText) findViewById(R.id.editTextUpdated);
		finish = (EditText) findViewById(R.id.editTextFinish);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != ENABLE_BLUETOOTH) {
			return;
		}
		if (resultCode == RESULT_OK) {
			startConnection();
		} else {
			Toast.makeText(this,
					"Bluetooth is NOT enabled\nExiting TimeMachine ...",
					Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		try {
			if (bluetoothHelper != null) {
				bluetoothHelper.shutdown();
			}
		} catch (IOException e) {
			Log.e(CLASS_ID, "IO error when shuting down");
		}

		finish();
	}

	private void startConnection() {
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();
		Log.i(CLASS_ID, "" + pairedDevices.size());
		for (BluetoothDevice device : pairedDevices) {
			Log.i(CLASS_ID, "[" + device.getName() + "]");

			if (DEVICE_NAME.equals(device.getName().trim())) {
				try {
					Log.i(CLASS_ID, "connecting");
					bluetoothHelper = new BluetoothHelper(device);
					Log.i(CLASS_ID, "connected");
				} catch (IOException e) {
					Toast.makeText(this, "Device not in range.",
							Toast.LENGTH_LONG).show();
					Log.e(CLASS_ID, "error connecting");
				}
			}
		}
	}

	public void sendStart(View view) {
		int trigerInt = 0;
		int releaseInt = 0;
		int totalInt = 0;
		try {
			trigerInt = (int) (Float.parseFloat(triger.getText().toString()) * 1000);
			releaseInt = (int) (Float.parseFloat(release.getText().toString()) * 1000);
			totalInt = Integer.parseInt(total.getText().toString());
		} catch (Exception e) {
			Toast.makeText(this, "Bad number", Toast.LENGTH_LONG).show();
			return;
		}
		String req = "{START" + trigerInt + "," + releaseInt + "," + totalInt
				+ "}";

		String res = sendMessage(req);
		updateUI(res);
	}

	public void sendStop(View view) {
		String req = "{STOP}";
		String res = sendMessage(req);
		updateUI(res);
	}

	public void sendCancel(View view) {
		triger.setText("");
		release.setText("");
		total.setText("");
	}

	public void sendQuery(View view) {
		String req = "{QUERY}";
		String res = sendMessage(req);
		updateUI(res);
	}

	private String sendMessage(String req) {
		String res = null;
		if (bluetoothHelper != null) {
			try {
				res = bluetoothHelper.sendMessage(req);
			} catch (IOException e) {
				Toast.makeText(this, "Error sending message.",
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, "Device not connected.", Toast.LENGTH_LONG)
					.show();
		}
		Log.i(CLASS_ID, "Result " + res);
		return res;
	}

	@SuppressLint("SimpleDateFormat")
	private void updateUI(String res) {
		if (res == null || res.split(",").length != 4) {
			Toast.makeText(this, "Bad response\nPlease Retry.",
					Toast.LENGTH_LONG);
			return;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Log.i(CLASS_ID, res);
		String[] result = res.split(",");
		int trigerInt = Integer.parseInt(result[0]);
		int releaseInt = Integer.parseInt(result[1]);
		int totalInt = Integer.parseInt(result[2]);
		int counterInt = Integer.parseInt(result[3]);

		triger.setText(""
				+ (trigerInt % 1000 != 0 ? trigerInt / 1000 + "." + trigerInt
						% 1000 : trigerInt / 1000));
		release.setText(""
				+ (releaseInt % 1000 != 0 ? releaseInt / 1000 + "."
						+ releaseInt % 1000 : releaseInt / 1000));
		total.setText("" + totalInt);
		progressBar
				.setProgress(totalInt == 0 ? 0 : counterInt * 100 / totalInt);
		counter.setText("" + counterInt);
		updated.setText(sdf.format(new Date()));
		finish.setText(sdf.format(new Date(new Date().getTime()
				+ (trigerInt + releaseInt) * (totalInt - counterInt))));

	}
}
