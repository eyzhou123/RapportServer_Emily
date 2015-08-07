import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class GUI extends JFrame {
	public static JButton button;
	public static JButton delete_button;
	public static JPanel button_panel;
	public static JPanel android_panel;
	public static JPanel webcam_panel;
	
	public GUI() {
		super("Server");
		setLayout(new FlowLayout());
		
		setBackground(Color.gray);
		Box box = Box.createVerticalBox();
    	// This button should be used when closing the server, because it 
 		// ensures that a video is saved first. 
 		button = new JButton("STOP SERVER");
 		button.setBackground(new Color(0, 229, 255));
 		button.setForeground(Color.BLACK);
 		button.setBorderPainted(false);
 		button.setOpaque(true);
 		button.setFont(button.getFont().deriveFont(Font.BOLD));
 		button.setFont(new Font("Arial", Font.BOLD, 21));
 		button.setAlignmentX(Component.CENTER_ALIGNMENT);
 		HandlerClass handler = new HandlerClass();
 		button.addActionListener(handler);
 		box.add(button);
	    
 		// This button can be used any time to clear the workspace 
 		// of intermediate/temporary files (does nothing if those files
 		// don't exist). 
 		delete_button = new JButton("Delete extra files");
 		delete_button.setBackground(new Color(201, 201, 201));
 		delete_button.setForeground(Color.BLACK);
 		delete_button.setBorderPainted(false);
 		delete_button.setOpaque(true);
 		delete_button.setFont(button.getFont().deriveFont(Font.BOLD));
 		delete_button.setFont(new Font("Arial", Font.PLAIN, 20));
 		delete_button.setAlignmentX(Component.CENTER_ALIGNMENT);
 		delete_button.addActionListener(new ActionListener() {
 		  public void actionPerformed(ActionEvent e) {
 		    deleteExtraFiles();
 		  }
 		});
 		
 		box.add(delete_button);
 		add(box);
	}
	
	public static void make_video_a() {
		// Take care of closing speakers/microphone 
        AudioServer.microphone.stop();
        AudioServer.microphone.close();
        
        // This will stop recording audio and write the file
		AudioServer.line.stop();
        AudioServer.line.drain();
        AudioServer.line.close();
	}
	
	// The following two methods are for when the client side closes. They deal with
	// making sure both audio and video are written before trying to merge. They also
	// will not exit the program.
	
	public static void make_video_v() {
		// Closes the recorder and writes the video file
		
		// Wait for audio to be written
		while (AudioServer.line.isOpen()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        
		Thread finish_recording_thread = new Thread(new Runnable() {
            public void run() {
            	if (SocketServer.recording) {
        			try {
        				SocketServer.recorder.stop();
        				SocketServer.recorder.release();
        				SocketServer.recording = false;
        				//System.out.println("Stopped recording video");
        			} catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
        				// TODO Auto-generated catch block
        				e1.printStackTrace();
        			}
        		}
            }
       });  
		finish_recording_thread.start();
		
	    // merge video and audio to one final video
		Runtime rt = Runtime.getRuntime();
		try {
			System.out.println("Merging WoZ audio and video files...");
			
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_h.mm.ssa");
			String timestamp = sdf.format(date);
			
////		the following can speed up the video if needed when merging (but only works directly from terminal)
	//		Process pr2 = rt.exec(MainProgram.path_to_ffmpeg + " -i " + SocketServer.path 
	//				+ "audio.wav -i " + SocketServer.path + "video.mp4 -strict experimental -vf 'setpts=0.7*PTS'" 
	//				+ SocketServer.path + timestamp + ".mp4");
			
			Process makeWoZVid = rt.exec(MainProgram.path_to_ffmpeg + " -i " + SocketServer.path 
					+ "audio.wav -i " + SocketServer.path + "video.mp4 -strict experimental " 
					+ SocketServer.path + timestamp + ".mp4");
			
			try {
				makeWoZVid.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// EZ: Can automatically delete files, but I have commented this out in case the other files want 
			// to be kept.
//    		deleteExtraFiles();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// This will take the 'android' folder containing all the images from the stream, save the
	// android audio to a file, and then merge all these together to make the android user video
	public static void androidVideoProcessing() {
		Runtime rt = Runtime.getRuntime();
		try {
			 System.out.println("Saving android audio to file...");
			 InputStream b_in = new ByteArrayInputStream(SocketServerAndroidAudio.b_out.toByteArray());
			try {
			        DataOutputStream dos = new DataOutputStream(new FileOutputStream(
			                SocketServer.path + "temp_android_audio.bin"));
			        dos.write(SocketServerAndroidAudio.b_out.toByteArray());
			        AudioFormat format = new AudioFormat(44100.0F, 16, 1, true, false);
			        AudioInputStream stream = new AudioInputStream(b_in, format,
			        		SocketServerAndroidAudio.b_out.toByteArray().length / format.getFrameSize());
			        File file = new File(SocketServer.path + "android_audio.wav");
			        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
			        System.out.println("File saved: " + file.getName() + ", bytes: "
			                + SocketServerAndroidAudio.b_out.toByteArray().length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.out.println("Converting android frames to video...");
			 Process framesToVideo = rt.exec(MainProgram.path_to_ffmpeg + " -r 12 -f image2 -s 1920x1000 -i " + SocketServer.path 
						+ "android/image%d.jpg -vcodec libx264 -crf 25 " + SocketServer.path + "android.mp4"); 
			 try {
				 framesToVideo.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 System.out.println("Rotating android video...");
			 Process rotating = rt.exec(MainProgram.path_to_ffmpeg + " -i " + SocketServer.path + "android.mp4 -vf transpose=2 "
					 + SocketServer.path + "android_video.mp4"); 
			 try {
				 rotating.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			System.out.println("Combining android video and audio...");
			Process makeAndroidVideo = rt.exec(MainProgram.path_to_ffmpeg + " -i " + SocketServer.path 
					+ "android_audio.wav -i " + SocketServer.path + "android_video.mp4 -c:v copy -c:a aac -strict experimental " 
					+ SocketServer.path + "android_" + SocketServerAndroid.timestamp + ".mp4");
			try {
				makeAndroidVideo.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteExtraFiles() {
		// Delete extra stuff?
		System.out.println("Deleting extra files...");
		File dir = new File(SocketServer.path + "android/");
		if (dir.exists()) {
			if (dir.isDirectory()) {
		        String[] children = dir.list();
		        for(String c: children){
		            File currentFile = new File(dir.getPath(), c);
		            currentFile.delete();
		        }
		    }
		    dir.delete();
		}
		File temp_woz_vid = new File(SocketServer.path + "video.mp4");
		if (temp_woz_vid.exists()) {
			temp_woz_vid.delete();
		}
		File temp_woz_audio = new File(SocketServer.path + "audio.wav");
		if (temp_woz_audio.exists()) {
			temp_woz_audio.delete();
		}
		File temp_android_audio = new File(SocketServer.path + "android_audio.wav");
		if (temp_android_audio.exists()) {
			temp_android_audio.delete();
		}
		File temp_android_audio_bin = new File(SocketServer.path + "temp_android_audio.bin");
		if (temp_android_audio_bin.exists()) {
			temp_android_audio_bin.delete();
		}
		File temp_android_vid = new File(SocketServer.path + "android.mp4");
		if (temp_android_vid.exists()) {
			temp_android_vid.delete();
		}
		File temp_android_vid2 = new File(SocketServer.path + "android_video.mp4");
		if (temp_android_vid2.exists()) {
			temp_android_vid2.delete();
		}
	}
	
	
	// The following will write the files, wait for the two threads to end, and then merge them. 
	// Then, the program will exit.
	
	private class HandlerClass implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (!SocketServer.client_closed) {
				JOptionPane.showMessageDialog(null, "Closed server, saving video.");
				SocketServer.server_is_running = false;
				SocketServer.canvas.removeAll();
				
				AudioServer.line.stop();
		        AudioServer.line.drain();
		        AudioServer.line.close();
				
				try {
					MainProgram.server.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					MainProgram.audio_socket.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				// merge video and audio to one final video
				Runtime rt = Runtime.getRuntime();
				try {
					System.out.println("Merging audio and video files...");
					
					Date date = new Date();
		    		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_h.mm.ssa");
		    		String timestamp = sdf.format(date);
					
		    		Process makeWoZVideo2 = rt.exec(MainProgram.path_to_ffmpeg + " -i " + SocketServer.path 
							+ "audio.wav -i " + SocketServer.path + "video.mp4 -c:v copy -c:a aac -strict experimental " 
							+ SocketServer.path + timestamp + ".mp4");
		    		
		    		try {
		    			makeWoZVideo2.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    		
		    		androidVideoProcessing();
//		    		deleteExtraFiles();
						
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.exit(0);
			
		}
	}

	
}