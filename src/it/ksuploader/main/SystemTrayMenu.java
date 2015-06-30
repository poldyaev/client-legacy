package it.ksuploader.main;


import it.ksuploader.dialogs.SettingsDialog;
import it.ksuploader.utils.MyKeyListener;
import it.ksuploader.utils.Sound;
import it.ksuploader.utils.Zipper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Random;

import static java.awt.SystemTray.getSystemTray;
import static java.awt.SystemTray.isSupported;

public class SystemTrayMenu {

	private Clipboard clpbrd;
	private PopupMenu popupMenu;
	private final Sound suono;
    private SocketUploader socketUploader;
	private FtpUploader ftpup;
	private TrayIcon trayIcon;

	private MenuItem[] uploads;
	private MenuItem catturaArea;
	private MenuItem catturaDesktop;
	private MenuItem caricaFile;
	private MenuItem clipboard;

	public SystemTrayMenu() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
			Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
		}
		this.suono = new Sound();
		this.uploads = new MenuItem[5];

		Main.dialog.cleanObservers();
		if (Main.config.getFtpEnabled()){
			this.socketUploader = null;
			this.ftpup = new FtpUploader();
			Main.dialog.addObserver(this.ftpup);
		} else {
			this.ftpup = null;
			this.socketUploader = new SocketUploader();
			Main.dialog.addObserver(this.socketUploader);
		}

		for (int i = 0; i < uploads.length; i++) {
			uploads[i] = new MenuItem();
		}

		if (isSupported()) {
			try {
				clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				SystemTray systemTray = getSystemTray();

				trayIcon = new TrayIcon(new ImageIcon(getClass().getResource("/res/icon.png")).getImage(), "KSUploader");
				trayIcon.setImageAutoSize(true);

				popupMenu = new PopupMenu();
				catturaArea = new MenuItem();
				catturaDesktop = new MenuItem();
				caricaFile = new MenuItem();
				clipboard = new MenuItem();
				updateKeys();
				MenuItem settings = new MenuItem("Settings");
				MenuItem esci = new MenuItem("Quit");

				popupMenu.add("Recent Uploads");
				popupMenu.addSeparator();
				popupMenu.addSeparator();
				popupMenu.add(catturaArea);
				popupMenu.add(catturaDesktop);
				popupMenu.addSeparator();
				popupMenu.add(caricaFile);
				popupMenu.add(clipboard);
				popupMenu.addSeparator();
				popupMenu.add(settings);
				popupMenu.addSeparator();
				popupMenu.add(esci);

				// Gestione voci menu
				catturaArea.addActionListener(e -> uploadPartialScreen());
				catturaDesktop.addActionListener(e ->  {
					try {
						Thread.sleep(320);
						uploadCompleteScreen();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				});
				caricaFile.addActionListener(e -> sendFile());
				clipboard.addActionListener(e -> sendClipboard());

				settings.addActionListener(e -> {
					SettingsDialog configPanel = new SettingsDialog();
					configPanel.loadCurrentConfig();
					configPanel.setVisible(true);
					FtpSocketSwitch();
				});

				esci.addActionListener(e -> {
					getSystemTray().remove(trayIcon);
					Main.startUpCheck(Main.config.isStartUpEnabled());
                    socketUploader.closeSocket();
					System.exit(0);
				});

				trayIcon.setPopupMenu(popupMenu);
				systemTray.add(trayIcon);
			} catch (AWTException ex) {
				ex.printStackTrace();
				Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
			}
		}
	}

	private void FtpSocketSwitch() {
		Main.dialog.cleanObservers();
		if (Main.config.getFtpEnabled()){
			this.socketUploader.closeSocket();
			this.socketUploader = null;
			this.ftpup = new FtpUploader();
			Main.dialog.addObserver(this.ftpup);
		} else {
			this.ftpup = null;
			this.socketUploader = new SocketUploader();
			Main.dialog.addObserver(this.socketUploader);
		}
	}

	public void updateKeys() {
		catturaArea.setLabel(("Capture Area " + loadKey(Main.config.getKeyScreen())));
		catturaDesktop.setLabel("Capture Desktop " + loadKey(Main.config.getKeyCScreen()));
		caricaFile.setLabel("Upload File " + loadKey(Main.config.getKeyFile()));
		clipboard.setLabel("Upload Clipboard " + loadKey(Main.config.getKeyClipboard()));
	}

	private String loadKey(int keyNumber[]) {
		StringBuffer ret = new StringBuffer("(");
		for (int e : keyNumber) {
			ret.append(MyKeyListener.fromKeyToName.get(e) + "+");
		}
		ret.replace(0,ret.length(),ret.substring(0, ret.length() - 1));
		ret.append(")");
		return ret.toString();
	}

	public void uploadPartialScreen(){
        new SwingWorker() {
            @Override
            protected Void doInBackground(){
                MyScreen partialScreen = new MyScreen();

                // Nel caso in cui lo screen fosse annullato o 5x5
                if (partialScreen.selectionBounds == null || partialScreen.selectionBounds.width <= 5
                        || partialScreen.selectionBounds.height <= 5) {

                    // Annullo
                    Main.dialog.show("Caricamento annullato!", ":(", false);
					Main.dialog.destroy();
                } else {
					// Se FTP
					if (Main.config.getFtpEnabled()) {
						try {

							File tempFile = new File(Main.so.getTempDir() + "/" + System.currentTimeMillis() / 1000
									+ new Random().nextInt(999) + ".png");

							if (Main.config.isSaveEnabled()) {
								ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()).getSubimage(
												partialScreen.selectionBounds.x,
												partialScreen.selectionBounds.y,
												partialScreen.selectionBounds.width,
												partialScreen.selectionBounds.height), "png", tempFile);
								Main.myLog("[SocketUploader] MyScreen saved");
							}

							ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()).getSubimage(
									partialScreen.selectionBounds.x,
									partialScreen.selectionBounds.y,
									partialScreen.selectionBounds.width,
									partialScreen.selectionBounds.height), "png", tempFile);

							ftpup.setFilePath(tempFile);

							boolean res;
							res = ftpup.send();
							if (res) {
								Main.dialog.show("Screenshot Caricato!", ftpup.getLink(), true);
								history(ftpup.getLink());
								clpbrd.setContents(new StringSelection(ftpup.getLink()), null);
								tempFile.delete();
								suono.run();
							}
						} catch (AWTException | IOException e){
							e.printStackTrace();
						}
						// Se socket
                    } else {

                        File tempFile = new File(Main.so.getTempDir() + "/ksutemp.png");
                        try {


                            ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()).getSubimage(
									partialScreen.selectionBounds.x,
									partialScreen.selectionBounds.y,
									partialScreen.selectionBounds.width,
									partialScreen.selectionBounds.height), "png", tempFile);

                            if (Main.config.isSaveEnabled()) {
                                ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()).getSubimage(
										partialScreen.selectionBounds.x,
										partialScreen.selectionBounds.y,
										partialScreen.selectionBounds.width,
										partialScreen.selectionBounds.height), "png", new File(Main.config.getSaveDir() + "/" + System.currentTimeMillis() / 1000
                                                + new Random().nextInt(999) + ".png"));
                                Main.myLog("[SocketUploader] MyScreen saved");
                            }


                            socketUploader.setFilePath(tempFile.getPath());

                            boolean res;
                            res = socketUploader.send("img");
                            if (res) {
                                Main.dialog.show("Upload Completed!", socketUploader.getLink(), true);
                                history(socketUploader.getLink());
                                clpbrd.setContents(new StringSelection(socketUploader.getLink()), null);
                                tempFile.delete();
                                suono.run();
                            }
                        } catch (IOException | AWTException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
        }.execute();
    }

    public void uploadCompleteScreen(){
        new SwingWorker() {
            @Override
            protected Void doInBackground(){
                try {
                    boolean res;

                    // Se FTP
                    if (Main.config.getFtpEnabled()) {
						try {
							File tempFile = new File(Main.so.getTempDir() + "/" + System.currentTimeMillis() / 1000
									+ new Random().nextInt(999) + ".png" );

							if (Main.config.isSaveEnabled()) {
								ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()),
										"png",
										tempFile);
								Main.myLog("[SocketUploader] MyScreen saved");
							}
							ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()), "png", tempFile);

							ftpup.setFilePath(tempFile);

							res = ftpup.send();
							if (res) {
								Main.dialog.show("Upload Completed!", ftpup.getLink(), true);
								history(ftpup.getLink());
								clpbrd.setContents(new StringSelection(ftpup.getLink()), null);
								tempFile.delete();
								suono.run();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						// Se socket
                    } else {

                        File tempFile = new File(Main.so.getTempDir() + "/ksutemp.png");

                        try {
                            ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()), "png", tempFile);
                            if (Main.config.isSaveEnabled()) {
                                ImageIO.write(new Robot().createScreenCapture(Main.so.getScreenBounds()), "png",
                                        new File(Main.config.getSaveDir() + "/" + System.currentTimeMillis() / 1000 + ""
                                                + new Random().nextInt(999) + ".png"));
                                Main.myLog("[SocketUploader] MyScreen saved");
                            }

                            socketUploader.setFilePath(tempFile.getPath());
                            res = socketUploader.send("img");
                            if (res) {
                                Main.dialog.show("Upload Completed!", socketUploader.getLink(), true);
                                history(socketUploader.getLink());
                                clpbrd.setContents(new StringSelection(socketUploader.getLink()), null);
                                tempFile.delete();
                                suono.run();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (AWTException ex) {
                    ex.printStackTrace();
                    Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
                }
                return null;
            }
        }.execute();
    }

	public void sendFile() {
		try {
			JFileChooser selFile = new JFileChooser();
			selFile.setMultiSelectionEnabled(true);
			selFile.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			Action details = selFile.getActionMap().get("viewTypeDetails");
			details.actionPerformed(null);
			if (selFile.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                new SwingWorker() {
                    @Override
                    protected Void doInBackground(){
                        boolean res = false;
                        if (Main.config.getFtpEnabled()) {
                            // Se non finisce con .zip O ci sono più file

                            if (selFile.getSelectedFiles()[0].getName().toLowerCase().endsWith(".png") && selFile.getSelectedFiles().length == 1) {
                                ftpup.setFilePath(selFile.getSelectedFiles()[0].getPath());

                            } else if (!selFile.getSelectedFiles()[0].getName().endsWith(".zip") || selFile.getSelectedFiles().length > 1) {
								ftpup.setFilePath(new Zipper(selFile.getSelectedFiles()).toZip("ftp"));

                                // Altrimenti se finisce con .zip O è uno solo
                            } else if (selFile.getSelectedFiles()[0].getName().endsWith(".zip") && selFile.getSelectedFiles().length == 1) {
								ftpup.setFilePath(selFile.getSelectedFiles()[0].getPath());
                            }

                            res = ftpup.send();
                            if (res) {
                                Main.dialog.show("Upload Completed!", ftpup.getLink(), true);
                                history(ftpup.getLink());
                                clpbrd.setContents(new StringSelection(ftpup.getLink()), null);
                                suono.run();
                            }

                        } else {

                            if (selFile.getSelectedFiles()[0].getName().toLowerCase().endsWith(".png") && selFile.getSelectedFiles().length == 1) {
                                socketUploader.setFilePath(selFile.getSelectedFiles()[0].getPath());
                                res = socketUploader.send("img");

                            } else if (!selFile.getSelectedFiles()[0].getName().endsWith(".zip") || selFile.getSelectedFiles().length > 1) {
                                socketUploader.setFilePath(new Zipper(selFile.getSelectedFiles()).toZip("socket"));
                                res = socketUploader.send("file");

                            } else if (selFile.getSelectedFiles()[0].getName().endsWith(".zip") && selFile.getSelectedFiles().length == 1) {
                                socketUploader.setFilePath(selFile.getSelectedFiles()[0].getPath());
                                res = socketUploader.send("file");
                            }

                            if (res) {
                                Main.dialog.show("Upload Completed!", socketUploader.getLink(), true);
                                history(socketUploader.getLink());
                                clpbrd.setContents(new StringSelection(socketUploader.getLink()), null);
                                new File(Main.so.getTempDir(), "KStemp.zip").delete();
                                suono.run();
                            }

                        }
                        return null;
                    }
                }.execute();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
		}
	}

	private void history(String link) {
		popupMenu.remove(uploads[uploads.length - 1]);

		System.arraycopy(uploads, 0, uploads, 1, uploads.length - 1);
		uploads[0] = new MenuItem(link);
		uploads[0].addActionListener(e -> {
			try {
				Desktop.getDesktop().browse(new URI(e.getActionCommand()));
			} catch (URISyntaxException | IOException ex) {
				ex.printStackTrace();
				Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
			}
		});
		popupMenu.insert(uploads[0], 2);
	}

	public void sendClipboard() {
		boolean res;
		try {

			String clipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
					.getData(DataFlavor.stringFlavor);
			String fileName = System.currentTimeMillis() / 1000 + "" + new Random().nextInt(999);
			File f = new File(Main.so.getTempDir() + "/" + fileName + ".txt");
			Main.myLog(f.getPath());
			PrintWriter out = new PrintWriter(Main.so.getTempDir() + "/" + fileName + ".txt");
			out.println(clipboard);
			out.close();

			if (Main.config.getFtpEnabled()) {
				ftpup.setFilePath(f);
				res = ftpup.send();
				if (res) {
					Main.dialog.show("Upload Completed!", ftpup.getLink(), true);
					history(ftpup.getLink());
					clpbrd.setContents(new StringSelection(ftpup.getLink()), null);
					suono.run();
				}

			} else {

                socketUploader.setFilePath(f.getPath());
				res = socketUploader.send("txt");
				if (res) {
					Main.dialog.show("Upload Completed!", socketUploader.getLink(), true);
					history(socketUploader.getLink());
					clpbrd.setContents(new StringSelection(socketUploader.getLink()), null);
					f.delete();
					suono.run();
				}
			}

		} catch (UnsupportedFlavorException | IOException ex) {
			ex.printStackTrace();
			Main.dialog.show("Error!", "Error with clipboard!", false);
			Main.myErr(Arrays.toString(ex.getStackTrace()).replace(",", "\n"));
		}
	}
}
