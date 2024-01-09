
### Main Goals:
1. **Background Recording:** The app records audio in the background using a foreground service.
2. **Segmentation:** It allows the user to specify a segment duration (9 minutes, 2 hours, 10 hours) and save that duration as a segment in a different folder.

### Key Components:
- **MainActivity:** Contains buttons to trigger the duration selection dialog.
- **RecordingForegroundService:** Responsible for ongoing recording and handling the saving of segments.

### Logic Overview:
1. **MainActivity:**
   - Contains buttons to select the duration.
   - When a duration button is clicked, it triggers a dialog with a seek bar to set the desired duration.
   - Once confirmed, it sends an intent to the `RecordingForegroundService` with the specified start and end times.

2. **RecordingForegroundService:**
   - Receives the intent with start and end times for the segment.
   - Extracts the ongoing recording file.
   - Copies the specified segment duration from the ongoing recording to a new file in a different directory.

### Troubleshooting Steps:
1. **Intent Sending:** Confirm that the intents from MainActivity to RecordingForegroundService are sent with the correct data (start time, end time).
2. **Intent Handling:** Ensure the RecordingForegroundService correctly handles the incoming intents and processes the segment saving logic.
3. **File Operations:** Check the file handling logic in RecordingForegroundService, especially file creation, copying segments, and exception handling.
4. **Logging & Debugging:** Add logs at critical points in both MainActivity and RecordingForegroundService to trace the flow and verify if the intended functions are being executed.
