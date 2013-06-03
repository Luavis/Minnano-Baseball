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

import com.example.baseballgame.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		final EditText input = new EditText(this);
		builder = new AlertDialog.Builder(this);
		builder.setTitle("이름을 입력해 주세요");
		
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);
		
		builder.setPositiveButton("확인", new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {}
		});
		builder.setCancelable(false);

		dlg = builder.create();

		th2 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/setName?name=" + m_Text);
				try {
					HttpResponse response = MainActivity.httpclient.execute(newSession);
					InputStream contentStream = response.getEntity().getContent();
					StringBuffer out = new StringBuffer();
					byte[] buffer = new byte[4094];
					int readSize = 0;
					while ( (readSize = contentStream.read(buffer)) != -1) {
					    out.append(new String(buffer, 0, readSize));
					}
					contentStream.close();
					
					String result = out.toString();
					JSONObject ob = new JSONObject(result);
					String message = ob.getString("message");
					if(message.equals("success")) {
						userName = m_Text;
					}
					else if(message.equals("fail") && ob.getString("reason").equals("name exist")) {
						SplashActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SplashActivity.this, "이름이 이미 존재 합니다.", Toast.LENGTH_LONG).show();
							}
						});
					}
					else {
						SplashActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
								setResult(-1);
								finish();
							}
						});
					}
				} catch (Exception e) {
					SplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
							setResult(-1);
							finish();
						}
					});
				}
				
				SplashActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.d("Luavis", "Fuck Android");
						
						Intent intent = getIntent();
						//intent.putExtra("time", time);
						//intent.putExtra("userName", userName);
						//("msg", msg);
						for(int i = 0; i< msg.size(); i++) {
							MainActivity.msg.add(msg.get(i));
						}
						ChatMessage.me = userName;
						MainActivity.time = time;

						setResult(0, intent);

			        	finish();
					}
				});
			}
		});
		
		h = new Handler() {
			public void handleMessage(Message msg) {
				dlg.show();
				View button = ((AlertDialog)dlg).getButton(DialogInterface.BUTTON_POSITIVE);
				button.setOnClickListener(new View.OnClickListener() {
				  @Override
				  public void onClick(View v) {
					 m_Text = clearText(input.getText().toString());
			        if(m_Text.length() == 0) {
			        	Toast.makeText(SplashActivity.this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
			        }
			        else{
			        	th2.start();
			        	dlg.dismiss();
			        }
				  }});
			}
		};
		
		Thread th1 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/new");
				try {
					HttpResponse response = MainActivity.httpclient.execute(newSession);
					InputStream contentStream = response.getEntity().getContent();
					StringBuffer out = new StringBuffer();
					byte[] buffer = new byte[4094];
					int readSize = 0;
					while ( (readSize = contentStream.read(buffer)) != -1) {
					    out.append(new String(buffer, 0, readSize));
					}
					contentStream.close();
					
					String result = out.toString();
					JSONObject ob = new JSONObject(result);
					String message = ob.getString("message");
					if(message.equals("success")) {
						JSONArray j_msg = ob.getJSONArray("msg");
						for(int i = 0; i < j_msg.length(); i++) {
							JSONObject o = j_msg.getJSONObject(i);
							ChatMessage hash = new ChatMessage();
							try {
								hash.name = o.getString("name");
							}
							catch(Exception e) {
								hash.name = "";
							}
							try {
								hash.msg = o.getString("message");
							}
							catch(Exception e) {
								hash.msg = "";
							}
							try {
								hash.time = o.getLong("time");
							}
							catch(Exception e) {
								hash.time = 0;
							}

							msg.add(hash);
						}

						time = ob.getLong("time");
						SplashActivity.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								h.sendEmptyMessage(0);
							}
						});
					}
					else {
						SplashActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
								setResult(-1);
								finish();
							}
						});
					}
				} catch (Exception e) {
					Log.d("Luavis", "Here");
					Log.d("Luavis", e.getMessage());
					SplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(SplashActivity.this, "서버에러입니다. 잠시후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
							setResult(-1);
							finish();
						}
					});
				}
				//HttpGet setName = new HttpGet(url + "/setName");
			}
		});
		th1.start();
		
		//h.sendEmptyMessageDelayed(0, 2000);
	}
}
