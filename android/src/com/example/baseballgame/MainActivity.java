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
	
	Thread answerSendThread = new Thread(new Runnable() { // make thread for send to server
		@Override
		public void run() { // run!
			// TODO Auto-generated method stub
			String message = null; // message!

			backgroundThreadPause(); // background thread pause for safety
			try {
				// make message with user type answer and encode percentage encoding
				message = URLEncoder.encode(firstInput.getText().toString() + secondInput.getText().toString() + thirdInput.getText().toString(), "utf-8"); 
			} catch (UnsupportedEncodingException e1) { // when string that user type is not UTF-8 string
				// TODO Auto-generated catch block
				e1.printStackTrace();
				backgroundThreadResume(); //resume 
			} 

			// make url : time is recent time and message is user type answer
			HttpGet newSession = new HttpGet(MainActivity.url + "/answer?time=" + time + "&message=" + message);

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
				if(!ob.getString("message").equals("success")) { // when response is not success
					throw null; // throw! exception
				}
				else { // else
					MainActivity.this.runOnUiThread(new Runnable() { // start main thread to clear input text
						public void run() {
							isFirstInput = isSecondInput = isThirdInput = false; // set all clear!
							
							firstInput.setText("");   // all clear
							secondInput.setText(""); // all clear
							thirdInput.setText(""); // all clear
						}
					});
					if(ob.getBoolean("correct")==  true) { // if user answer correct?
						Toast.makeText(MainActivity.this, "축하합니다! 정답을 맞추셨습니다!", Toast.LENGTH_LONG).show();
					}
					
				}
			}
			catch(Exception e) { // if exception is occur
				MainActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
					public void run() {
						Toast.makeText(MainActivity.this, "서버와 연결이 끊켰습니다.\n 정답을 업로드 할 수 없습니다.", Toast.LENGTH_LONG).show(); // show toast
						//finish();
					}
				});
			}
			backgroundThreadResume(); // background thread resume!
		}
	});
	
	Thread messageSendThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			MainActivity.this.runOnUiThread(new Runnable() { // sending disable
				public void run() {
					msgSend.setEnabled(false);
					msgSend.setFocusable(false);
				}
			});

			String msg = msgText.getText().toString(); // get message
			if(msg.length() >= 256) { // if message string is too long
				MainActivity.this.runOnUiThread(new Runnable() { //start main thread to show toast
					public void run() {
						Toast toast = Toast.makeText(MainActivity.this, "너무 길어요!", Toast.LENGTH_SHORT); 
						toast.show();
					}
				});
			}
			else if(msg.length() > 0) {
				backgroundThreadPause(); // background thread pause for safety
				HttpGet newSession = null;
				try { // make url
					newSession = new HttpGet(MainActivity.url + "/write?time=" + URLEncoder.encode(time + "", "utf-8")+ "&message=" + URLEncoder.encode(msg, "utf-8"));
				} catch (UnsupportedEncodingException e1) { // text is not utf-8 character
					// TODO Auto-generated catch block
					e1.printStackTrace();
					backgroundThreadResume();
				}
				
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
					if(!ob.getString("message").equals("success")) { // when response is not success
						throw null; // throw! exception
					}
					else {// if success
						MainActivity.this.runOnUiThread(new Runnable() { //start main thread to clear message box
							public void run() { // clear text
								msgText.setText(""); // clear
							}
						});
					}
				}
				catch(Exception e) { // if exception is occur
					MainActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
						public void run() {
							MainActivity.this.runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									Toast.makeText(MainActivity.this, "메세지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show(); // show error meessage
								}
							});
						}
					});
				}

				backgroundThreadResume();
			}
			else {
				MainActivity.this.runOnUiThread(new Runnable() { // start main thread to show toast message
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(MainActivity.this, "텍스트를 입력해 주세요!", Toast.LENGTH_SHORT).show(); // show please input text
					}
				});
			}
			
			MainActivity.this.runOnUiThread(new Runnable() { // set re-sendable
				public void run() {
					msgSend.setEnabled(true);
					msgSend.setFocusable(true);
				}
			});
		}
	});
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPauseLock = new Object(); // Mutex Setting For Background Thread
		mPaused = false; // background Thread Watcher 
		mFinished = false; // background Thread Killer
		
		startActivityForResult(new Intent(MainActivity.this, SplashActivity.class), 1); // Start Activity For Splash activity(loading screen)
		setContentView(R.layout.activity_main); // inflate layout for activity

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN); // When Keyboard is shown, up screen
		Log.d("Luavis", "Start"); // Log
		
		firstInput = (EditText)findViewById(R.id.first_input); // find view by id
		secondInput = (EditText)findViewById(R.id.second_input); // find view by id
		thirdInput = (EditText)findViewById(R.id.third_input);
		msgText = (EditText)findViewById(R.id.msg_text); // find view by id
		chatList = (ListView)findViewById(R.id.listview); // find view by id
		msgSend = (Button)findViewById(R.id.msg_send); // find view by id
		
		chatList.setDivider(null); // chat list divider line remove 
		
		scrollBottomHandler = new Runnable() { // scroll chatting list to always bottom
	        @Override
	        public void run() { // run!
	            // Select the last row so it will scroll into view...
	            chatList.setSelection(adapter.getCount() - 1); // make list show last component
	        }
	    };

	    player = MediaPlayer.create(this, R.raw.bgsound); // backgroundsound play ready
        player.setLooping(true); // Set looping
        player.setVolume(100,100); // volume set highest
        player.start(); // start!

		TextWatcher watcher = new TextWatcher() { // watching baseball game edit text is full?
		    @Override
		    public void afterTextChanged(Editable s) {} // blank
		 
		    @Override
		    public void beforeTextChanged(CharSequence s, int start, int count, int after){} // blank
		 
		    @Override 
		    public void onTextChanged(CharSequence s, int start, int before, int count) 
		    {	//when editText's text is changed

		         if (firstInput.isFocused()) { // if first editText is editted?
	        		 isFirstInput = (firstInput.getText().toString().length() != 0); // is removed or is inputed text?
		         }
		         else if(secondInput.isFocused()) { // if second editText is editted?
		        	 isSecondInput = (secondInput.getText().toString().length() != 0);// is removed or is inputed text?
		         }
		         else if(thirdInput.isFocused()) { // if third editText is editted?
		        	 isThirdInput = (thirdInput.getText().toString().length() != 0);// is removed or is inputed text?
		         }
		         
		         if(isFirstInput && isSecondInput && isThirdInput) { // if all editText is full
		        	 //Send! to server
		        	 answerSendThread .start(); // thread call! synchroningly
		         }
		    }
		};
		
		firstInput.addTextChangedListener(watcher); // text watcher work!
		secondInput.addTextChangedListener(watcher); // text watcher work!
		thirdInput.addTextChangedListener(watcher); // text watcher work!

		msgSend.setOnClickListener(new OnClickListener() { // when message send button click!
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				messageSendThread.start(); // message send thread
			}
		});
		adapter = new ChatListAdapter(this, android.R.layout.simple_list_item_1, msg); // chat list adapter!
		//new ArrayAdapter<ChatMessage>(this, android.R.layout.simple_list_item_1, msg);
		chatList.setAdapter(adapter); // set adapter!
	}
	
	private void scrollChatListViewToBottom() {
	    chatList.post(scrollBottomHandler); // scoll to bottom handler act with chat list in main thread 
	}
	
	private void backgroundThreadPause() { // background thread pause
		synchronized (mPauseLock) { // pause lock synchronized in critical section
            mPaused = true; // set true to pause 
            mPauseLock.notifyAll(); // notify it is end of critical section
        }
	}
	private void backgroundThreadResume() { // background thread resume
		synchronized (mPauseLock) { // pause lock synchronized in critical section 
            mPaused = false; // set false to resume
            mPauseLock.notifyAll(); // notify it is end of critical section
        }
	}
	
	@Override
	protected void onPause() { // when activity is pausing
		// TODO Auto-generated method stub
		super.onPause();
		player.pause(); // background sound pause
		backgroundThreadPause(); // background thread pause
	}
	
	@Override
	protected void onResume() { // when activity is resuming
		// TODO Auto-generated method stub
		super.onResume();
		player.start(); // backgound sound resume
		backgroundThreadResume(); //background thread resume
	}
	
	@Override
	protected void onStop() { // when activity is restarting
		// TODO Auto-generated method stub
		super.onStop();
		player.pause(); // background sound pause
		backgroundThreadPause(); // background thread pause
	}
	
	@Override
	protected void onRestart() { // when activity is restarting
		// TODO Auto-generated method stub
		super.onRestart();
		player.start(); // backgroun sound resume
		backgroundThreadResume(); // background thread resume
	}
	
	@Override
	protected void onDestroy() { // when activity is destroying
		// TODO Auto-generated method stub
		player.stop(); // stop player
        player.release(); // release from memory

		Thread th1 = new Thread(new Runnable() { // make thread
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGet newSession = new HttpGet(MainActivity.url + "/leave"); // notify user close app
				try {
					MainActivity.httpclient.execute(newSession); // excute request
				}
				catch(Exception e) {}
			}
		});
		th1.start(); // start thread
		
		super.onDestroy(); // destroy
		mFinished = true; // kill background thread!
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		//super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == 0) {
			userName = ChatMessage.me; // set user name
			backgroundThread = new Thread(new Runnable() { // create background thread
				
				@Override
				public void run() { // run!
					// TODO Auto-generated method stub
					while(!mFinished) {
						synchronized (mPauseLock) { // critical section
			                while (mPaused) {
			                    try {
			                       mPauseLock.wait(500); // wait! and check!
			                    } catch (InterruptedException e) {
			                    }
			                }
			            }
						HttpGet newSession = new HttpGet(MainActivity.url + "/readNew?time=" + time); // get server time and new message
						try {
							/** 나는 이것을 경이로운 방법으로 설명할 수 있으나, 자습 시간이 충분하지 않아 설명하지 않는다.
							 *  - 페르마의 마지막정리에서 인용(?)
							 */
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
									Toast.makeText(MainActivity.this, "???? ???淡? ???? ?颯??求?.\n ?母? ?천??? ?玲???.", Toast.LENGTH_LONG).show();
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
