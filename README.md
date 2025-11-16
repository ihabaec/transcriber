# YouTube Transcriber

A Spring Boot web application that converts YouTube videos to text transcriptions using OpenAI's Whisper model.

## Features

- Simple web interface for entering YouTube URLs
- Automatic audio extraction from YouTube videos
- AI-powered speech-to-text transcription using Whisper
- Copy-to-clipboard functionality for transcriptions
- Automatic cleanup of temporary files
- Progress indicators and error handling

## Prerequisites

Before running this application, ensure you have the following installed:

### Required Software

1. **Java 17** or higher
   ```bash
   java -version
   ```

2. **Maven** (included via Maven Wrapper)

3. **Python 3** with the following packages:
   ```bash
   pip install yt-dlp
   pip install openai-whisper
   ```

4. **FFmpeg** (required by yt-dlp for audio processing)
   - Ubuntu/Debian: `sudo apt install ffmpeg`
   - macOS: `brew install ffmpeg`
   - Windows: Download from [ffmpeg.org](https://ffmpeg.org/download.html)

## Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd transcriber
   ```

2. Install Python dependencies:
   ```bash
   pip install yt-dlp openai-whisper
   ```

3. Navigate to the demo directory:
   ```bash
   cd demo
   ```

4. Build the project:
   ```bash
   ./mvnw clean install
   ```

## Running the Application

1. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

2. Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```

3. Enter a YouTube URL and click "Transcribe Video"

## How It Works

1. **Audio Extraction**: The application uses `yt-dlp` to download and extract audio from the provided YouTube URL
2. **Audio Processing**: The audio is converted to WAV format and limited to the first 10 minutes for efficient processing
3. **Transcription**: OpenAI's Whisper (base model) processes the audio and generates a text transcription
4. **Cleanup**: Temporary audio and transcription files are automatically deleted after processing

## Project Structure

```
transcriber/
├── demo/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/transribe/demo/
│   │   │   │   ├── controller/
│   │   │   │   │   └── TranscriptionController.java
│   │   │   │   ├── service/
│   │   │   │   │   └── TranscriptionService.java
│   │   │   │   └── demo.java
│   │   │   └── resources/
│   │   │       ├── static/
│   │   │       │   └── index.html
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
└── README.md
```

## API Endpoints

### POST /transcribe

Transcribes a YouTube video to text.

**Request:**
```
Content-Type: application/x-www-form-urlencoded
youtubeUrl=https://www.youtube.com/watch?v=VIDEO_ID
```

**Response:**
```json
{
  "success": true,
  "transcription": "The transcribed text content..."
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Error message description"
}
```

## Configuration

The application uses the following default settings:
- **Port**: 8080
- **Whisper Model**: base (can be changed to: tiny, small, medium, large)
- **Audio Format**: WAV
- **Video Length Limit**: First 10 minutes
- **File Size Limit**: 100 MB
- **Temporary Directory**: System temp directory

To modify these settings, edit [TranscriptionService.java](demo/src/main/java/com/transribe/demo/service/TranscriptionService.java).

## Troubleshooting

### "yt-dlp is not installed"
Install yt-dlp: `pip install yt-dlp`

### "OpenAI Whisper is not installed"
Install Whisper: `pip install openai-whisper`

### "FFmpeg not found"
Install FFmpeg using your system's package manager

### Transcription takes too long
- Try using a shorter video
- Use a smaller Whisper model (e.g., "tiny" instead of "base")
- Ensure your system has sufficient RAM and CPU resources

### Audio extraction fails
- Verify the YouTube URL is valid and the video is accessible
- Check that yt-dlp is up to date: `pip install --upgrade yt-dlp`
- Ensure FFmpeg is properly installed and in your system PATH

## Technologies Used

- **Spring Boot 3.2.0** - Backend framework
- **Java 17** - Programming language
- **OpenAI Whisper** - AI speech recognition model
- **yt-dlp** - YouTube video/audio downloader
- **FFmpeg** - Audio/video processing
- **HTML/CSS/JavaScript** - Frontend interface

## Limitations

- Videos are limited to the first 10 minutes for processing efficiency
- Audio files larger than 100 MB are rejected
- Processing time depends on video length and system resources
- Requires active internet connection to download videos

## License

This project is open source and available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
