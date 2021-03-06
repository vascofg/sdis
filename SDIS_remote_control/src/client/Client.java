package client;

import gui.StatusGUI;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.JOptionPane;

import message.Message;
import clipboard.ClipboardFlavorChangeListener;
import clipboard.ClipboardHandler;
import clipboard.ClipboardListener;

public class Client {
	static DatagramSocket socket;
	static DatagramPacket packet;
	static InetAddress initiatorAddress;
	static Robot r;
	static StatusGUI statusGUI;
	static final int port = 44444;
	static final int clipboardPort = 44445;
	static boolean controllingScreen;

	static Point screenCenter;

	static EdgeDetect edgeDetect;
	static EventHandler eventHandler;
	static MessageListener messageListener;
	static MessageSender messageSender;
	static ClipboardListener fileListener;
	static ClipboardHandler fileHandler;

	static Dimension screenRes;

	public static void init() {
		try {
			socket = new DatagramSocket(port);
			r = new Robot();

			controllingScreen = false;

			screenRes = Toolkit.getDefaultToolkit().getScreenSize();
			screenCenter = new Point(screenRes.width / 2, screenRes.height / 2);

			eventHandler = new EventHandler();
			eventHandler.start();

			edgeDetect = new EdgeDetect();
			edgeDetect.pause();
			edgeDetect.start(); // start paused

			messageListener = new MessageListener();
			messageListener.start();

			messageSender = new MessageSender();
			messageSender.start();

			statusGUI = StatusGUI.getInstance();
			statusGUI.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					exit();
					super.windowClosed(e);
				}
			});
			statusGUI.getClipboard.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new Thread() {
						@Override
						public void run() {
							try {
								fileListener.requestClipboard();
							} catch (ConnectException e) {
								JOptionPane.showMessageDialog(null,
										"File transfer failed", "Timeout",
										JOptionPane.ERROR_MESSAGE);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}.start();
				}
			});

			fileHandler = new ClipboardHandler(clipboardPort);
			fileHandler.start();

			fileListener = new ClipboardListener(clipboardPort);
			fileListener.start();

			Toolkit.getDefaultToolkit()
					.getSystemClipboard()
					.addFlavorListener(
							new ClipboardFlavorChangeListener(messageSender));

			statusGUI.setActivity(false);
		} catch (SocketException | AWTException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void exit() {
		try {
			statusGUI.dispose();
		} catch (NullPointerException e) {
		}
		eventHandler.interrupt();
		edgeDetect.interrupt();
		messageListener.interrupt();
		messageSender.interrupt();
		fileHandler.interrupt();
		fileListener.interrupt();
	}

	public static void showStatusGUI() {
		statusGUI.setVisible(true);
	}

	public static void leaveScreen() {
		Client.statusGUI.setActivity(false);
		controllingScreen = false;
		edgeDetect.pause();
	}

	public static void joinScreen() {
		Client.statusGUI.setActivity(true);
		controllingScreen = true;
		edgeDetect.unpause();
	}

	static void onEdge(byte edge, int percentage) {
		messageSender.addMessage(Message.edge(edge, percentage));
	}
}
