package com.example.baseballgame;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SplashActivity extends Activity {
	static public String clearText(String msg) {
		String ret = "";

		for(int i = 0; i < msg.length(); i++) {
			char c = msg.charAt(i);
			
			if(c == ' ')
				continue;
			else 
				ret += c;
		}
		
		return ret;
	}
	public ArrayList<ChatMessage> msg = new ArrayList<ChatMessage>();

	public String userName;
	public long time;
	private String m_Text = "";
	private AlertDialog.Builder builder;
	private Handler h;
	private Dialog dlg;
	private Thread th2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // super inherit
		setContentView(R.layout.activity_splash); // inflate layout for activity
		final EditText input = new EditText(this); // create EditText to input name
		builder = new AlertDialog.Builder(this); // create alert builder
		builder.setTitle("이름을 입력해 주세요"); // set name for user
		
		input.setInputType(InputType.TYPE_CLASS_TEXT); // input text type setting 
		builder.setView(input); // insert EditText to input name to builder
		
		builder.setPositiveButton("확인", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {}
		}); // positive button create (not implement onClick for checck name)
		builder.setCancelable(false); // alert dialog cancelable false

		dlg = builder.create(); // create dialog

		th2 = new Thread(new Runnable() { // create thread for name setting
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/setName?name=" + m_Text); // create name url which set name
				try { // for application safety
					HttpResponse response = MainActivity.httpclient.execute(newSession); // send connection
					InputStream contentStream = response.getEntity().getContent(); // content stream
					StringBuffer out = new StringBuffer(); // string builder to create result with buffer
					byte[] buffer = new byte[4094]; // create buffer
					int readSize = 0; // read size
					while ( (readSize = contentStream.read(buffer)) != -1) { // exit when reading complete
					    out.append(new String(buffer, 0, readSize)); // stringBuilder append buffer
					}
					contentStream.close(); // stream close
					
					String result = out.toString(); // String builder create String 
					JSONObject ob = new JSONObject(result); // JSON Parsing
					String message = ob.getString("message"); // JSON Object get message
					if(message.equals("success")) { //check status message is success (if not finish app)
						userName = m_Text; // setting name
					}
					else if(message.equals("fail") && ob.getString("reason").equals("name exist")) { // if reason is name already existed, ask user to re-input name
						SplashActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
							@Override
							public void run() { // main thread run!
								Toast.makeText(SplashActivity.this, "이름이 이미 존재 합니다.", Toast.LENGTH_LONG).show(); // show Toast
								h.sendEmptyMessage(0); // show 
							}
						});
					}
					else { // kill thread
						SplashActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
							@Override
							public void run() {
								Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
								setResult(-1); // if result is -1, MainActivity finish his activity
								finish(); // finish actvity!
							}
						});
					}
				} catch (Exception e) {
					SplashActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
						@Override
						public void run() { 
							Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show(); // is connection error occur, show toast
							setResult(-1); // if result is -1, MainActivity finish his activity
							finish(); // finish actvity!
						}
					});
				}
				
				SplashActivity.this.runOnUiThread(new Runnable() { // start main thread to finish activity
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.d("Luavis", "Fuck Android"); // 흔한 프로그래머의 분노
						
						Intent intent = getIntent(); // get intent but it is not actually used
						//intent.putExtra("time", time);
						//intent.putExtra("userName", userName);
						//("msg", msg);
						for(int i = 0; i< msg.size(); i++) { // getting message from result json object 
							MainActivity.msg.add(msg.get(i)); // input message
						}
						ChatMessage.me = userName; // set my name
						MainActivity.time = time; //set server time

						setResult(0, intent); // result setting 0 which mead activity is finish well

			        	finish(); // finish activity
					}
				});
			}
		});
		
		h = new Handler() { // handler create for if name is already exist
			public void handleMessage(Message msg) { 
				dlg.show(); // show dialog
				View button = ((AlertDialog)dlg).getButton(DialogInterface.BUTTON_POSITIVE); // get button (name input)
				button.setOnClickListener(new View.OnClickListener() { // click listner
				  @Override
				  public void onClick(View v) { // onClick
					 m_Text = clearText(input.getText().toString()); // editText input | clear text(remove white space)
			        if(m_Text.length() == 0) { // check length
			        	Toast.makeText(SplashActivity.this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show(); // show toast
			        }
			        else{ // if name is valid
			        	th2.start(); // send it to server
			        	dlg.dismiss(); // dialog close
			        }
				  }});
			}
		};
		
		Thread th1 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/new"); // show new! session create!!
				try {
					HttpResponse response = MainActivity.httpclient.execute(newSession); // send connection
					InputStream contentStream = response.getEntity().getContent(); // content stream
					StringBuffer out = new StringBuffer(); // string builder to create result with buffer
					byte[] buffer = new byte[4094]; // create buffer
					int readSize = 0; // read size
					while ( (readSize = contentStream.read(buffer)) != -1) { // exit when reading complete
					    out.append(new String(buffer, 0, readSize)); // stringBuilder append buffer
					}
					contentStream.close(); // stream close
					
					String result = out.toString(); // String builder create String 
					JSONObject ob = new JSONObject(result); // JSON Parsing
					String message = ob.getString("message"); // JSON Object get message
					if(message.equals("success")) { // check status message equal success
						JSONArray j_msg = ob.getJSONArray("msg"); // get now message
						for(int i = 0; i < j_msg.length(); i++) { // message getting 
							JSONObject o = j_msg.getJSONObject(i); // get!
							ChatMessage hash = new ChatMessage(); // make it! (reason of variable name is hash is first time it is hash Object )
							try {
								hash.name = o.getString("name"); // get name
							}
							catch(Exception e) {
								hash.name = ""; // if there is not name set black string 
							}
							
							try {
								hash.msg = o.getString("message"); // get message 
							}
							catch(Exception e) {
								hash.msg = ""; // if there is not message set black string 
							}
							try {
								hash.time = o.getLong("time"); // get when it is writting
							}
							catch(Exception e) {
								hash.time = 0; // if there is not message set black string 
							}

							msg.add(hash); // add it to messages array list
						}

						time = ob.getLong("time"); // get now time
						SplashActivity.this.runOnUiThread(new Runnable() { // show dialog to setting name
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								h.sendEmptyMessage(0); // send message to handler to invoke
							}
						});
					}
					else {
						SplashActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show(); // show toast
								setResult(-1); // result set -1
								finish(); // finish activity
							}
						});
					}
				} catch (Exception e) {
					SplashActivity.this.runOnUiThread(new Runnable() { // connection error 
						@Override
						public void run() {
							Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
							setResult(-1); // result set -1
							finish(); // finish activity
						}
					});
				}
			}
		});
		th1.start(); // start Thread!!
	}
}
