package activehistorian_android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.example.activehistorianbottomsup.R;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoRecording extends Activity implements SurfaceHolder.Callback, OnInfoListener, OnErrorListener{

	private Button record;
	private Button stop;
	private TextView recordingstatus;
	private VideoView videoview;
	private SurfaceHolder holder;
	private Camera camera;
	private MediaRecorder recorder;
	private String outputFileName;
	private String outputLeafName;
	private String TAG;
	private DropboxAPI<?> mApi;
	
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    
	@Override
	protected void onCreate(Bundle savedInstanceState){
		this.mApi = MainActivity.getMApi();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videorecording);
		record = (Button) findViewById(R.id.Record);
		stop = (Button) findViewById(R.id.Stop);
		recordingstatus = (TextView) findViewById(R.id.Status);
		videoview = (VideoView) findViewById(R.id.Video);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		record.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				recordingstatus.setText("Recording");
				init();
				recorder.start();
			}});
		
		stop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				recordingstatus.setText("Not Recording");
				if(recorder != null){
					try{recorder.stop();} 
					catch (IllegalStateException e){}
					releaseRecorder();
					releaseCamera();
					putFile();
					Toast toast = Toast.makeText(VideoRecording.this,"Upload completed",Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		});
	}
	
	private void releaseCamera() {
		if(camera != null){
			try
			{
				camera.reconnect();
			}catch(IOException e){}
			camera.release();
			camera = null;
		}
	}
	
	private void releaseRecorder() {
		if(recorder != null){
			recorder.release();
			recorder = null;
		}
	}

	public void init(){
		if(recorder != null){ return;}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String currentDateandTime = sdf.format(new Date());
		outputLeafName = currentDateandTime +".mp4";
		outputFileName = Environment.getExternalStorageDirectory() + "/" + outputLeafName;;
		File outfile  = new File(outputFileName);
		
		try{
			camera.stopPreview();
			camera.unlock();
			recorder = new MediaRecorder();
			recorder.setCamera(camera);
			recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			recorder.setVideoSize(175, 144);
			recorder.setVideoFrameRate(15);
			recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setMaxDuration(360000);
			recorder.setPreviewDisplay(holder.getSurface());
			recorder.setOutputFile(outputFileName);
			recorder.prepare();
		}catch(Exception e){}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {return false;}

	@Override
	public void onInfo(MediaRecorder arg0, int arg1, int arg2) {}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try{
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch(IOException e){}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {}
	
	@Override
	protected void onResume(){
		super.onResume();
		if(!initCamera()){finish();}
	}
	
	private boolean initCamera() {
		try{
			camera = Camera.open();
			Camera.Parameters camParams = camera.getParameters();
			camera.lock();
			holder = videoview.getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		} catch (RuntimeException e){return false;}
		return true;
	}
	
	public void putFile(){

		File temp = new File(outputFileName);
		FileInputStream inputStream = null;
		
		try {
		    inputStream = new FileInputStream(temp);
            DropboxAPI.Entry newEntry = mApi.putFileOverwrite(outputLeafName, inputStream, temp.length(), null);
		    Log.i("DbExampleLog", "The uploaded file's rev is: " + newEntry.rev);
		} catch (DropboxUnlinkedException e) {
		    Log.e("DbExampleLog", "User has unlinked.");
		} catch (DropboxException e) {
		    Log.e("DbExampleLog", "Something went wrong while uploading.");
		} catch (FileNotFoundException e) {
		    Log.e("DbExampleLog", "File not found.");
		} finally {
		    if (inputStream != null) {
		        try {
		            inputStream.close();
		        } catch (IOException e) {}
		    }
		}	
	}
}
