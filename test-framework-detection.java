import com.example.gitmigrator.service.FrameworkDetectionService;
import com.example.gitmigrator.model.FrameworkType;
import java.io.File;

public class TestFrameworkDetection {
    public static void main(String[] args) {
        // Test framework detection with current repository
        File testDir = new File(".");
        FrameworkDetectionService service = new FrameworkDetectionService();
        FrameworkDetectionService.DetectionResult result = service.detectFramework(testDir);
        
        System.out.println("Primary Framework: " + result.getPrimaryFramework());
        System.out.println("Is Monorepo: " + result.isMonorepo());
        System.out.println("Components found: " + result.getComponents().size());
        
        for (FrameworkDetectionService.ComponentResult component : result.getComponents()) {
            System.out.println("  - " + component);
        }
    }
}