import org.vosk.Model;
import org.vosk.Recognizer;

/** Verifies an installed model with the bundled engine, without opening a microphone. */
class VoskModelLoadSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected model directory");
        try (Model model = new Model(args[0]);
             Recognizer recognizer = new Recognizer(model, 16000)) {
            byte[] silence = new byte[32000];
            recognizer.acceptWaveForm(silence, silence.length);
            System.out.println(recognizer.getFinalResult());
            System.out.println("PASS: model loaded and decoded PCM silence; human speech and microphone not tested.");
        }
    }
}
