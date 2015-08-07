

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;









import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class SocketServerAndroidAudio extends Thread {
	private ServerSocket mServer;
	private DataListener mDataListener;
	private BufferManager mBufferManager;
	private int width;
	private int height;
	public static boolean android_is_streaming = true;
	public static SourceDataLine sourceDataLine;

	AudioInputStream audioInputStream;
	static AudioInputStream ais;
	static AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
	static boolean status = true;
	static int port = 50005;
	static int sampleRate = 44100;
	static int bufferSize = 9728;
	private static final int audioStreamBufferSize = bufferSize * 20;
    static byte[] audioStreamBuffer = new byte[audioStreamBufferSize];
    private static int audioStreamBufferIndex = 0;
    public static SourceDataLine speakers;
    public static byte[] audio_data = null;
    public static ByteArrayOutputStream b_out = null;
	
	public SocketServerAndroidAudio() {
	    
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();

		System.out.println("android audio server's waiting");

		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		Socket socket = null;
		ByteArrayOutputStream byteArray = null;
		try {
			mServer = new ServerSocket(50005);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		byte[] buff = new byte[256];
		byte[] imageBuff = null;
		byte[] length_buff = new byte[4];
		int len = 0;
		String msg = null;
		
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
		try {
			
			speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			speakers.open(format);
	        speakers.start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		// the server_is_running variable keeps server open for new client connections
		while(SocketServer.server_is_running) {
			
			try {
				socket = mServer.accept();
				System.out.println("new android audio socket");
				inputStream = new BufferedInputStream(socket.getInputStream());
				outputStream = new BufferedOutputStream(socket.getOutputStream());
				int just_read = 0;
				b_out = new ByteArrayOutputStream();
				while(SocketServer.server_is_running) {
					//read audio buffer length 
					int length_bytes_read = 0;
					
					while (length_bytes_read < 4) {
						just_read = inputStream.read(length_buff, length_bytes_read, 4 - length_bytes_read);
						if (just_read < 0) {
							break;
						}
						length_bytes_read += just_read;
						
					}
					if (just_read < 0) {
						break;
					}
					int updated_length = bytesToInt(length_buff);
					audio_data = new byte[updated_length];
					//Log.d("ERRORCHECK", "will read: " + updated_length + "bytes");
					
					// read audio bytes into audio_data buffer
					int audio_bytes_read = 0;
					while (audio_bytes_read < updated_length) {
						just_read = inputStream.read(audio_data, audio_bytes_read, updated_length - audio_bytes_read);
						audio_bytes_read += just_read;
						if (just_read < 0) {
							break;
						}
				        
				        speakers.write(audio_data, 0, just_read);
				        b_out.write(audio_data, 0, just_read);
					}
					if (just_read < 0) {
						break;
					}
//					File newPath = new File(SocketServer.path + SocketServerAndroid.timestamp + "/android_audio.wav");
//					
//
//					AudioFormat format = new AudioFormat(8000f, 16, 1, true, false);
//					ByteArrayInputStream b_in 	= new ByteArrayInputStream(b_out.toByteArray());
//			    	AudioInputStream	 ais 	= new AudioInputStream(b_in, format, audio_data.length);
//			    	AudioSystem.write(ais, AudioFileFormat.Type.WAVE, newPath);
					
					
				}
				GUI.androidVideoProcessing();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

}

	
	
	
	public static void toSpeaker(byte soundbytes[]) {
	    try {

	        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
	        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

	        sourceDataLine.open();

//	        FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
//	        float minimum = volumeControl.getMinimum();
//	        float maximum = volumeControl.getMaximum();
//	        float newValue = (float)(minimum + 100.0f * (maximum - minimum) / 100.0f);
//	        
//	        volumeControl.setValue(newValue);

	        sourceDataLine.start();
	        sourceDataLine.open(format);

	        sourceDataLine.start();

	        System.out.println("format? :" + sourceDataLine.getFormat());

	        sourceDataLine.write(soundbytes, 0, soundbytes.length);
	        System.out.println(soundbytes.toString());
	        sourceDataLine.drain();
	        sourceDataLine.close();
	    } catch (Exception e) {
	        System.out.println("Not working in speakers...");
	        e.printStackTrace();
	    }
	}
	
	public int bytesToInt(byte[] int_bytes) throws IOException {
		return ByteBuffer.wrap(int_bytes).getInt();
	}

			
}
