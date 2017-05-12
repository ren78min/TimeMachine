package me.mren.timemachine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothHelper {

	private static final UUID SERIAL_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String CLASS_ID = "BluetoothHelper";

	private BluetoothDevice device = null;
	private BluetoothSocket socket = null;
	private OutputStream out = null;
	private InputStream in = null;
	private boolean initialized = false;

	public BluetoothHelper(BluetoothDevice device) throws IOException {
		this.device = device;
		startup();
	}

	synchronized void startup() throws IOException {
		StringBuilder errorBuilder = new StringBuilder();
		try {
			socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
			socket.connect();
			in = socket.getInputStream();
			out = socket.getOutputStream();
			initialized = true;
		} catch (IOException e) {
			errorBuilder.append("Error initalize Bluetooth IO (");
			errorBuilder.append(e.getMessage());
			errorBuilder.append(")\n");

			if (out != null) {
				try {
					out.close();
				} catch (IOException e1) {
					errorBuilder.append("Error closing OutputStream (");
					errorBuilder.append(e1.getMessage());
					errorBuilder.append(")\n");
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					errorBuilder.append("Error closing InputStream (");
					errorBuilder.append(e1.getMessage());
					errorBuilder.append(")\n");
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e1) {
					errorBuilder.append("Error closing BluetoothSocket (");
					errorBuilder.append(e1.getMessage());
					errorBuilder.append(")\n");
				}
			}
			throw new IOException(errorBuilder.toString());
		}
	}

	synchronized void shutdown() throws IOException {
		StringBuilder errorBuilder = new StringBuilder();
		if (out != null) {
			try {
				out.close();
			} catch (IOException e1) {
				errorBuilder.append("Error closing OutputStream (");
				errorBuilder.append(e1.getMessage());
				errorBuilder.append(")\n");
			}
		}

		if (in != null) {
			try {
				in.close();
			} catch (IOException e1) {
				errorBuilder.append("Error closing InputStream (");
				errorBuilder.append(e1.getMessage());
				errorBuilder.append(")\n");
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e1) {
				errorBuilder.append("Error closing BluetoothSocket (");
				errorBuilder.append(e1.getMessage());
				errorBuilder.append(")\n");
			}
		}
		initialized = false;
		String errorMessage = errorBuilder.toString();
		if (errorMessage.length() > 0) {
			Log.e(CLASS_ID, "Error in shutdown()");
			throw new IOException(errorMessage);
		}
	}

	public synchronized String sendMessage(String message) throws IOException {
		if (device == null) {
			throw new IOException("Device not valid.");
		}

		// try 3 times to send the message
		for (int i = 1; i <= 3; i++) {
			try {
				if (!initialized) {
					startup();
				}
				out.write(message.getBytes());
				break;
			} catch (IOException e) {
				Log.e(CLASS_ID, "Error sending message.");
				try {
					shutdown();
				} catch (IOException e1) {
					Log.e(CLASS_ID, "Cann't shutdown after sending failed.");

				}
				if (i == 3) {
					Log.e(CLASS_ID, "Sending failed 3 times.");
					throw e;
				}
			}
		}

		Log.i(CLASS_ID, "Command sent.");

		StringBuilder sb = null;
		String lastString = null;

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Log.e(CLASS_ID, "Sleep interrupted");
		}

		while (in.available() > 0) {
			int c = in.read();
			switch (c) {
			case '<':
				sb = new StringBuilder();
				break;
			case '>':
				lastString = sb == null ? null : sb.toString();
				break;
			default:
				sb.append((char) c);
				break;
			}
		}
		return lastString;
	}

}
