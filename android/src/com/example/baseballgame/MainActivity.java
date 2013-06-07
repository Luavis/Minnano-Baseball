package com.example.baseballgame;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	EditText firstInput;
	EditText secondInput;
	EditText thirdInput;
	ListView chatList;
	
	EditText msgText;
	Button msgSend;
	
	private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;
    
	Boolean isFirstInput = false;
	Boolean isSecondInput = false;
	Boolean isThirdInput = false;
	String userName = "";
	public static ArrayList<ChatMessage> msg = new ArrayList<ChatMessage>();
	public static long time = 0;
	Thread backgroundThread;
	Runnable scrollBottomHandler;
	static DefaultHttpClient httpclient = new DefaultHttpClient();
	static String url = "http://baseball.luavis.kr";
	//ArrayAdapter<ChatMessage> adapter;
	ChatListAdapter adapter;
	MediaPlayer player;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPauseLock = new Object(); // Mutex Setting For Background Thread
		mPaused = false; // background Thread Watcher 
		mFinished = false; // background Thread Killer
		
		startActivityForResult(new Intent(MainActivity.this, SplashActivity.class), 1);
		setContentView(R.layout.activity_main);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		Log.d("Luavis", "Start");
		
		firstInput = (EditText)findViewById(R.id.first_input);
		secondInput = (EditText)findViewById(R.id.second_input);
		thirdInput = (EditText)findViewById(R.id.third_input);
		msgText = (EditText)findViewById(R.id.msg_text);
		chatList = (ListView)findViewById(R.id.listview);
		msgSend = (Button)findViewById(R.id.msg_send);
		chatList.setDivider(null);
		
		scrollBottomHandler = new Runnable() {
	        @Override
	        public void run() {
	            // Select the last row so it will scroll into view...
	            chatList.setSelection(adapter.getCount() - 1);
	        }
	    };

	    player = MediaPlayer.create(this, R.raw.bgsound);
        player.setLooping(true); // Set looping
        player.setVolume(100,100);
        player.start();

		TextWatcher watcher = new TextWatcher() {
		    @Override
		    public void afterTextChanged(Editable s) {}
		 
		    @Override
		    public void beforeTextChanged(CharSequence s, int start, int count, int after){}
		 
		    @Override
		    public void onTextChanged(CharSequence s, int start, int before, int count)
		    {
		         if (firstInput.isFocused()) {
	        		 isFirstInput = (firstInput.getText().toString().length() != 0);
		         }
		         else if(secondInput.isFocused()) {
		        	 isSecondInput = (secondInput.getText().toString().length() != 0);
		         }
		         else if(thirdInput.isFocused()) {
		        	 isThirdInput = (thirdInput.getText().toString().length() != 0);
		         }
		         
		         if(isFirstInput && isSecondInput && isThirdInput) {
		        	 //Send!
		        	 
		        	 Thread th1 = new Thread(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								String message = null;
								try {
									message = URLEncoder.encode(firstInput.getText().toString() + secondInput.getText().toString() + thirdInput.getText().toString(), "utf-8");
								} catch (UnsupportedEncodingException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								
								HttpGet newSession = new HttpGet(MainActivity.url + "/answer?time=" + time + "&message=" + message);
								backgroundThreadPause();
								
								try {
									HttpResponse response = MainActivity.httpclient.execute(newSession);
									InputStream contentStream = response.getEntity().getContent();
									StringBuffer out = new StringBuffer();
									byte[] buffer = new byte[4094];
									int readSize = 0;
									while ( (readSize = contentStream.read(buffer)) != -1) {
									    out.append(new String(buffer, 0, readSize));
									}
									
									String ret = out.toString();
									JSONObject ob = new JSONObject(ret);
									if(!ob.getString("message").equals("success")) {
										throw null;
									}
									else {
										MainActivity.this.runOnUiThread(new Runnable() {
											public void run() {
												isFirstInput = isSecondInput = isThirdInput = false;
												
												firstInput.setText("");
												secondInput.setText("");
												thirdInput.setText("");
											}
										});
										if(ob.getBoolean("correct")==  true) {
											MainActivity.this.runOnUiThread(new Runnable() {
												public void run() {
													Toast.makeText(MainActivity.this, "축하합니다! 정답을 맞추셨습니다!", Toast.LENGTH_LONG).show();
												}
											});
										}
										
									}
								}
								catch(Exception e) {
									MainActivity.this.runOnUiThread(new Runnable() {
										public void run() {
											Toast.makeText(MainActivity.this, "서버와 연결이 끊켰습니다.\n 정답을 업로드 할 수 없습니다.", Toast.LENGTH_LONG).show();
											//finish();
										}
									});
								}
								backgroundThreadResume();
							}
						});
						
						th1.start();
		         }
		    }
		};
		
		firstInput.addTextChangedListener(watcher);
		secondInput.addTextChangedListener(watcher);
		thirdInput.addTextChangedListener(watcher);

		msgSend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Thread th22 = new Thread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						String msg = msgText.getText().toString();
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								msgSend.setEnabled(false);
								msgSend.setFocusable(false);
							}
						});
						if(msg.length() >= 256) {
							Toast toast = Toast.makeText(MainActivity.this, "너무 길어요!", Toast.LENGTH_SHORT);
							toast.show();
							toast = null;
						}
						else if(msg.length() > 0) {
							HttpGet newSession = null;
							try {
								newSession = new HttpGet(MainActivity.url + "/write?time=" + URLEncoder.encode(time + "", "utf-8")+ "&message=" + URLEncoder.encode(msg, "utf-8"));
							} catch (UnsupportedEncodingException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
	
							backgroundThreadPause();
							
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
								
								String ret = out.toString();
								JSONObject ob = new JSONObject(ret);
								if(!ob.getString("message").equals("success")) {
									throw new Exception("Message is fail!");
								}
								else {
									MainActivity.this.runOnUiThread(new Runnable() {
										public void run() {
											msgText.setText("");
										}
									});
								}
								
							}
							catch(Exception e) {
								MainActivity.this.runOnUiThread(new Runnable() {
									public void run() {
										MainActivity.this.runOnUiThread(new Runnable() {
											
											@Override
											public void run() {
												// TODO Auto-generated method stub
												Toast.makeText(MainActivity.this, "메세지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
											}
										});
									}
								});
							}

							backgroundThreadResume();
						}
						else {
							MainActivity.this.runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									Toast.makeText(MainActivity.this, "텍스트를 입력해 주세요!", Toast.LENGTH_SHORT).show();
								}
							});
						}
						
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								msgSend.setEnabled(true);
								msgSend.setFocusable(true);
							}
						});
					}
				});
					
				th22.start();
			}
		});
		adapter = new ChatListAdapter(this, android.R.layout.simple_list_item_1, msg);//new ArrayAdapter<ChatMessage>(this, android.R.layout.simple_list_item_1, msg);
		chatList.setAdapter(adapter);
	}
	
	private void scrollChatListViewToBottom() {
	    chatList.post(scrollBottomHandler);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		player.pause();
		backgroundThreadPause();
	}
	
	private void backgroundThreadPause() {
		synchronized (mPauseLock) {
            mPaused = true;
            mPauseLock.notifyAll();
        }
	}
	private void backgroundThreadResume() {
		synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		player.start();
		backgroundThreadResume();
	}
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		player.start();
		backgroundThreadResume();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		player.stop();
        player.release();

		Thread th1 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/new");
				try {
					MainActivity.httpclient.execute(newSession);
				}
				catch(Exception e) {}
			}
		});
		th1.start();
		
		super.onDestroy();
		mFinished = true; // kill thread!
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		//super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == 0) {
			
			//Bundle extras = data.getExtras();
			//time = data.getLongExtra("time", 0);
			//userName = data.getStringExtra("userName");
			userName = ChatMessage.me;
/*
			ArrayList<ChatMessage> arr = data.getParcelableArrayListExtra("msg");
			for(int i = 0; i < arr.size(); i++) {
				msg.add(arr.get(i));
			}*/
			
			//Log.d("Luavis", data.getStringExtra("userName"));
			MainActivity.this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					adapter.notifyDataSetChanged();
					scrollChatListViewToBottom();
				}
			});
			
			backgroundThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(!mFinished) {
						synchronized (mPauseLock) {
			                while (mPaused) {
			                    try {
			                       mPauseLock.wait(500);
			                    } catch (InterruptedException e) {
			                    }
			                }
			            }
						HttpGet newSession = new HttpGet(MainActivity.url + "/readNew?time=" + time);
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
							
							String ret = out.toString();
							JSONObject ob = new JSONObject(ret);
							if(ob.getString("message").equals("success")) {
								try {
									time = ob.getLong("time");
								}
								catch(Exception e) {}
								try {
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

									Collections.sort(msg, new ChatMessage.MessageComparator());
									MainActivity.this.runOnUiThread(new Runnable() {
										
										@Override
										public void run() {
											// TODO Auto-generated method stub
											adapter.notifyDataSetChanged();
											scrollChatListViewToBottom();
										}
									});
								}
								
								catch(Exception e) {}
								
							}
							else
								throw null;
						}
						catch(Exception e) {
							MainActivity.this.runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									Toast.makeText(MainActivity.this, "서버 접속에 실패 했습니다.\n 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
									//finish();
								}
							});
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
			backgroundThread.start();
		}
		else if(resultCode == -1) {
			finish();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
