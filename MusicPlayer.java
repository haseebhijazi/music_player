import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MusicPlayer extends JFrame {
    private List<MusicTrack> playlist;
    private JList<String> playlistList;
    private JPanel playlistPanel;
    private DefaultListModel<String> playlistListModel;
    private int currentTrackIndex;
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton nextButton;
    private JButton previousButton;
    private JButton addButton;
    private JSlider progressSlider;
    private JLabel trackInfoLabel;
    private Clip clip;
    private boolean isPaused;
    private boolean isPlaying; 

    public MusicPlayer() {
        playlist = new ArrayList<>();
        currentTrackIndex = 0;
        isPaused = false;
        isPlaying = false;

        // Create GUI components
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        nextButton = new JButton("Next");
        previousButton = new JButton("Previous");
        addButton = new JButton("Add");
        progressSlider = new JSlider(0, 100);
        trackInfoLabel = new JLabel();
        playlistListModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistListModel);

        playlistPanel = new JPanel(new BorderLayout());
        playlistPanel.add(new JScrollPane(playlistList), BorderLayout.CENTER);


        // Set up the layout
        setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(previousButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(addButton); 
        JPanel playlistPanel = new JPanel(new BorderLayout());
        playlistPanel.add(new JScrollPane(playlistList), BorderLayout.CENTER);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(playlistPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER); // buttons and playlist
        add(progressSlider, BorderLayout.SOUTH);
        add(trackInfoLabel, BorderLayout.NORTH);

        // Add event listeners
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isPaused) {
                    resumeCurrentTrack();
                } else {
                    playCurrentTrack();
                }
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pauseCurrentTrack();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(true);
                addButton.setEnabled(true);
            }
        });
        
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopCurrentTrack();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                addButton.setEnabled(true);
            }
        });
        

        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playNextTrack();
            }
        });

        previousButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playPreviousTrack();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSongsToPlaylist();
            }
        });

        progressSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (clip != null && !progressSlider.getValueIsAdjusting()) {
                    long newPosition = (long) (clip.getMicrosecondLength() * (progressSlider.getValue() / 100.0));
                    clip.setMicrosecondPosition(newPosition);
                }
            }
        });

        // Set up the frame
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void playCurrentTrack() {
        if (playlist.isEmpty()) {
            return;
        }
    
        MusicTrack currentTrack = playlist.get(currentTrackIndex);
        File audioFile = currentTrack.getFile();
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
    
            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "Unsupported audio file format: " + audioFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioStream);
            clip.start();
            updateGUI(currentTrack);
            progressSlider.setMaximum(100);
            isPlaying = true; // Update playing state
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            addButton.setEnabled(false);
            startProgressSliderUpdateThread();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error playing the track: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    private void pauseCurrentTrack() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            isPaused = true;
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }
    }

    private void resumeCurrentTrack() {
        if (clip != null && isPaused) {
            clip.start();
            isPaused = false;
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
        }
    }

    private void stopCurrentTrack() {
        if (clip != null) {
            clip.stop();
            clip.setMicrosecondPosition(0);
            isPaused = false;
            isPlaying = false;
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }
    }

    private void playNextTrack() {
        if (currentTrackIndex < playlist.size() - 1) {
            stopCurrentTrack();
            currentTrackIndex++;
            playCurrentTrack();
        }
    }

    private void playPreviousTrack() {
        if (currentTrackIndex > 0) {
            stopCurrentTrack();
            currentTrackIndex--;
            playCurrentTrack();
        }
    }

     private void updatePlaylistList() {
        playlistListModel.clear();
        for (MusicTrack track : playlist) {
            playlistListModel.addElement(track.getTitle());
        }
    }

    private void updateGUI(MusicTrack currentTrack) {
        trackInfoLabel.setText("Now playing: " + currentTrack.getTitle() + " - " + currentTrack.getArtist());
        trackInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        trackInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        trackInfoLabel.setBackground(Color.LIGHT_GRAY);
        trackInfoLabel.setOpaque(true); 
        trackInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        playlistPanel.setBackground(Color.WHITE); // Set background color of the playlistPanel
        playlistPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // Set a border for the playlistPanel

        playlistList.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font and font size of the playlist items
        playlistList.setSelectionBackground(Color.GRAY); 
        
        trackInfoLabel.setText("Now playing: " + currentTrack.getTitle() + " | Artist: " + currentTrack.getArtist());
        playButton.setEnabled(!isPaused);
        pauseButton.setEnabled(isPaused);
        stopButton.setEnabled(true);
        updatePlaylistList();
    }

    private void startProgressSliderUpdateThread() {
        Thread updateThread = new Thread(new Runnable() {
            public void run() {
                while (clip.isRunning()) {
                    long currentPosition = clip.getMicrosecondPosition();
                    int progress = (int) (currentPosition * 100 / clip.getMicrosecondLength());
                    progressSlider.setValue(progress);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                progressSlider.setValue(progressSlider.getMinimum());
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }
        });
        updateThread.start();
    }

    private void addSongsToPlaylist() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                String title = file.getName(); // Use the file name as the default title
                String artist = ""; // You can prompt the user to enter the artist name here if needed
                addToPlaylist(new MusicTrack(title, artist, file));
            }
        }
    }

    public void addToPlaylist(MusicTrack track) {
        playlist.add(track);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MusicPlayer player = new MusicPlayer();
                player.setVisible(true);
            }
        });
    }
}

class MusicTrack {
    private String title;
    private String artist;
    private File file;

    public MusicTrack(String title, String artist, File file) {
        this.title = title;
        this.artist = artist;
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public File getFile() {
        return file;
    }
}