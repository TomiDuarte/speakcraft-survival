import org.vosk.*;
import javax.sound.sampled.*;
import java.io.File;
import com.google.gson.JsonParser;

/** Development-only decoder smoke test. Never packaged in the mod; never opens a microphone. */
class VoskFileSmoke {
    public static void main(String[] args) throws Exception {
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        boolean matched = false;
        try (Model model = new Model(args[0]);
             Recognizer recognizer = new Recognizer(model, 16000);
             AudioInputStream audio = AudioSystem.getAudioInputStream(new File(args[1]))) {
            if (audio.getFormat().getSampleRate() != 16000 || audio.getFormat().getChannels() != 1
                    || audio.getFormat().getSampleSizeInBits() != 16) throw new IllegalArgumentException("Expected PCM 16 kHz mono 16 bit");
            byte[] bytes = new byte[4096]; int count;
            while ((count = audio.read(bytes)) != -1) {
                if (recognizer.acceptWaveForm(bytes, count)) matched |= inspect(recognizer.getResult());
            }
            matched |= inspect(recognizer.getFinalResult());
        }
        if (!matched) throw new AssertionError("The real Vosk decoder did not recognize the expected Spanish word");
        System.out.println("PASS: actual Vosk Spanish model recognized synthetic Windows speech; microphone not tested.");
    }
    private static boolean inspect(String json) {
        String text = JsonParser.parseString(json).getAsJsonObject().get("text").getAsString();
        System.out.println("Vosk transcript: " + text);
        return text.strip().equalsIgnoreCase("diamante");
    }
}
