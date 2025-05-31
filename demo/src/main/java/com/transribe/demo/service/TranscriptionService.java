package com.transribe.demo.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TranscriptionService {

    private final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    public String transcribeYouTubeVideo(String youtubeUrl) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        String audioFile = null;
        
        try {
            // Step 1: Check if required tools are installed
            checkRequiredTools();
            
            // Step 2: Extract audio from YouTube video (limit to first 10 minutes for testing)
            audioFile = extractAudioFromYouTube(youtubeUrl, sessionId);
            
            // Step 3: Transcribe audio to text
            String transcription = transcribeAudio(audioFile);
            
            return transcription;
            
        } finally {
            // Cleanup temporary files
            if (audioFile != null) {
                cleanupFile(audioFile);
            }
        }
    }

    private void checkRequiredTools() throws Exception {
        // Check if yt-dlp is installed
        if (!isCommandAvailable("yt-dlp")) {
            throw new RuntimeException("yt-dlp is not installed. Please install it with: pip install yt-dlp");
        }
        
        // Check if whisper is installed
        if (!isCommandAvailable("whisper")) {
            throw new RuntimeException("OpenAI Whisper is not installed. Please install it with: pip install openai-whisper");
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            // Try different possible locations for the command
            String[] possibleCommands = {
                command,
                "python -m " + command,
                "python3 -m " + command,
                "/usr/local/bin/" + command,
                System.getProperty("user.home") + "/.local/bin/" + command
            };
            
            for (String cmd : possibleCommands) {
                try {
                    ProcessBuilder pb;
                    if (cmd.startsWith("python")) {
                        // For Python module commands
                        String[] parts = cmd.split(" ");
                        pb = new ProcessBuilder(parts[0], parts[1], parts[2], "--version");
                    } else {
                        pb = new ProcessBuilder(cmd, "--version");
                    }
                    
                    Process process = pb.start();
                    boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                    if (finished && process.exitValue() == 0) {
                        System.out.println("Found " + command + " at: " + cmd);
                        return true;
                    }
                } catch (Exception e) {
                    // Continue trying other locations
                }
            }
            
            // Special check for whisper as Python module
            if (command.equals("whisper")) {
                return checkWhisperAsPythonModule();
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkWhisperAsPythonModule() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "-c", "import whisper; print('Whisper available')");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                System.out.println("Found whisper as Python module");
                return true;
            }
        } catch (Exception e) {
            // Try python3
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", "-c", "import whisper; print('Whisper available')");
                Process process = pb.start();
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    System.out.println("Found whisper as Python3 module");
                    return true;
                }
            } catch (Exception e2) {
                // Continue
            }
        }
        return false;
    }

    private String extractAudioFromYouTube(String youtubeUrl, String sessionId) throws Exception {
        String outputTemplate = TEMP_DIR + "/audio_" + sessionId + ".%(ext)s";
        System.out.println("Starting audio extraction for session: " + sessionId);

        String[][] commands = {
            {"yt-dlp"},
            {"python", "-m", "yt_dlp"},
            {"python3", "-m", "yt_dlp"}
        };

        for (String[] baseCmd : commands) {
            try {
                // Construct full command list
                List<String> command = new ArrayList<>(List.of(baseCmd));
                command.addAll(List.of(
                    "--extract-audio",
                    "--audio-format", "wav",
                    "--audio-quality", "0",
                    "--postprocessor-args", "ffmpeg:-t 600",
                    "--output", outputTemplate,
                    "--no-playlist",
                    "--format", "140",
                    youtubeUrl
                ));

                ProcessBuilder pb = new ProcessBuilder(command);
                return executeAudioExtraction(pb, sessionId);
            } catch (Exception e) {
                System.out.println(String.join(" ", baseCmd) + " failed, trying next...");
            }
        }

        throw new RuntimeException("Could not run yt-dlp. Please ensure it's installed and accessible.");
    }

    
    private String executeAudioExtraction(ProcessBuilder pb, String sessionId) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Read output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("yt-dlp: " + line);
            }
        }
        
        boolean finished = process.waitFor(600, TimeUnit.SECONDS); // 10 minute timeout
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("YouTube download timed out after 10 minutes. Try a shorter video.");
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Failed to extract audio. Output: " + output.toString());
        }
        
        // Find the downloaded file
        String actualPath = findGeneratedAudioFile(sessionId);
        if (actualPath == null) {
            throw new RuntimeException("Audio file not found. Output: " + output.toString());
        }
        
        System.out.println("Audio extraction completed: " + actualPath);
        return actualPath;
    }

    private String transcribeAudio(String audioFilePath) throws Exception {
        System.out.println("Starting transcription of: " + audioFilePath);
        
        // Check file size
        File audioFile = new File(audioFilePath);
        long fileSizeBytes = audioFile.length();
        long fileSizeMB = fileSizeBytes / (1024 * 1024);
        
        System.out.println("Audio file size: " + fileSizeMB + " MB");
        
        if (fileSizeMB > 100) {
            throw new RuntimeException("Audio file too large (" + fileSizeMB + " MB). Try a shorter video.");
        }
        
        // Try different ways to run whisper
        ProcessBuilder pb = null;
        
        // Try whisper command directly
        try {
            pb = new ProcessBuilder(
                "whisper",
                audioFilePath,
                "--model", "base",
                "--output_format", "txt",
                "--output_dir", TEMP_DIR,
                "--verbose", "True"
            );
            return executeTranscription(pb, audioFilePath);
        } catch (Exception e) {
            System.out.println("Direct whisper command failed, trying python -m whisper...");
        }
        
        // Try python -m whisper
        try {
            pb = new ProcessBuilder(
                "python", "-m", "whisper",
                audioFilePath,
                "--model", "base",
                "--output_format", "txt",
                "--output_dir", TEMP_DIR,
                "--verbose", "True"
            );
            return executeTranscription(pb, audioFilePath);
        } catch (Exception e) {
            System.out.println("python -m whisper failed, trying python3 -m whisper...");
        }
        
        // Try python3 -m whisper
        try {
            pb = new ProcessBuilder(
                "python3", "-m", "whisper",
                audioFilePath,
                "--model", "base",
                "--output_format", "txt",
                "--output_dir", TEMP_DIR,
                "--verbose", "True"
            );
            return executeTranscription(pb, audioFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Could not run whisper. Please ensure it's installed and accessible. " +
                "Try running 'whisper --help' or 'python -m whisper --help' in your terminal.");
        }
    }
    
    private String executeTranscription(ProcessBuilder pb, String audioFilePath) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Read output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("Whisper: " + line);
            }
        }
        
        boolean finished = process.waitFor(1800, TimeUnit.SECONDS); // 30 minute timeout
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Transcription timed out after 30 minutes. Output: " + output.toString());
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Failed to transcribe audio. Output: " + output.toString());
        }
        
        // Read the transcription result
        String transcriptionFile = findTranscriptionFile(audioFilePath);
        if (transcriptionFile == null) {
            throw new RuntimeException("Transcription file not found. Output: " + output.toString());
        }
        
        String transcription = Files.readString(Paths.get(transcriptionFile));
        cleanupFile(transcriptionFile);
        
        System.out.println("Transcription completed successfully");
        return transcription.trim();
    }

    private String findGeneratedAudioFile(String sessionId) {
        File tempDir = new File(TEMP_DIR);
        File[] files = tempDir.listFiles((dir, name) -> 
            name.contains("audio_" + sessionId) && (name.endsWith(".wav") || name.endsWith(".mp3")));
        
        if (files != null && files.length > 0) {
            return files[0].getAbsolutePath();
        }
        
        // Also check for files without session ID (yt-dlp might name them differently)
        files = tempDir.listFiles((dir, name) -> 
            name.startsWith("audio_") && (name.endsWith(".wav") || name.endsWith(".mp3")));
        
        return files != null && files.length > 0 ? files[0].getAbsolutePath() : null;
    }

    private String findTranscriptionFile(String audioFilePath) {
        String tempBaseName = new File(audioFilePath).getName();
        // Remove extension
        int lastDot = tempBaseName.lastIndexOf('.');
        final String baseName;
        if (lastDot > 0) {
            baseName = tempBaseName.substring(0, lastDot);
        } else {
            baseName = tempBaseName;
        }
        
        File tempDir = new File(TEMP_DIR);
        File[] files = tempDir.listFiles((dir, name) -> 
            name.startsWith(baseName) && name.endsWith(".txt"));
        
        return files != null && files.length > 0 ? files[0].getAbsolutePath() : null;
    }

    private void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Failed to cleanup file: " + filePath + " - " + e.getMessage());
        }
    }
}