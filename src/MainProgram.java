import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class MainProgram extends JPanel implements DataListener {
	public static SocketServer server = new SocketServer();
	public static AudioServer audio_socket = new AudioServer();
	public static SocketServerAndroid android_server = new SocketServerAndroid();
	public static SocketServerAndroidAudio android_audio_server = new SocketServerAndroidAudio();
	public static boolean exit = false;
	public static String path_to_ffmpeg = "/usr/local/bin/ffmpeg";
	
	private LinkedList<BufferedImage> mQueue = new LinkedList<BufferedImage>();
	private static final int MAX_BUFFER = 15;
           
    BufferedImage mImage, mLastFrame;
    
    public MainProgram() {
	   android_server.setOnDataListener(this);
       android_server.start();
       android_audio_server.start();
    }
    
	public static void main (String [] args) {
		GUI gui = new GUI();
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setSize(750, 400);
		gui.setVisible(true);
	
		server.start();
		audio_socket.start();

        gui.add(new MainProgram());
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
		    public void run() {
		        System.out.println("Server exited.");
		    }
		});
		
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
		try {
			MainProgram.android_server.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (AudioServer.line.isRunning()) {
			AudioServer.line.stop();
	        AudioServer.line.drain();
	        AudioServer.line.close();
		}
		
		// 1. merge WoZer video and audio to one final video
		// 2. merge saved android stream images into video
		Runtime rt = Runtime.getRuntime();
		try {
			System.out.println("Merging audio and video files");
			
			Date date = new Date();
    		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_h.mm.ssa");
    		String timestamp = sdf.format(date);
			

			Process pr1 = rt.exec(path_to_ffmpeg + " -i " + SocketServer.path 
					+ "audio.wav -i " + SocketServer.path + "video.mp4 -c:v copy -c:a aac -strict experimental " 
					+ SocketServer.path + timestamp + ".mp4");
			
		    Process pr2 = rt.exec(MainProgram.path_to_ffmpeg + " -r 20 -f image2 -s 1920x1000 -i " + SocketServer.path 
					+ timestamp + "/image%d.jpg -vcodec libx264 -crf 25 " + SocketServer.path + timestamp 
					+ "/android_" + timestamp + ".mp4"); 
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void updateUI(BufferedImage bufferedImage) {
        synchronized (mQueue) {
        	if (mQueue.size() ==  MAX_BUFFER) {
        		mLastFrame = mQueue.poll();
        	}	
        	mQueue.add(bufferedImage);
        }
        repaint();
    }
	
	public Dimension getPreferredSize() {
        if (mImage == null) {
             return new Dimension(400, 300); // init window size
        } else {
           return new Dimension(mImage.getWidth(null), mImage.getHeight(null));
       }
    }
	
	public void onDirty(BufferedImage bufferedImage) {
		// TODO Auto-generated method stub
		updateUI(bufferedImage);
	}

    public void paint(Graphics g) {
        synchronized (mQueue) {
        	if (mQueue.size() > 0) {
        		mLastFrame = mQueue.poll();
        	}	
        }
      
        if (mLastFrame != null) {
        	double rotationRequired = Math.toRadians(270);
            double locationX = mLastFrame.getWidth() / 2;
            double locationY = mLastFrame.getHeight() / 2;
            AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        	g.drawImage(op.filter(mLastFrame, null), 0, 0, null);
        }
        else if (mImage != null) {
            g.drawImage(mImage, 0, 0, null);
        }
    }
	
	
	
}
