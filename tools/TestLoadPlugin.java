import org.openstreetmap.josm.plugins.PluginInformation;

public class TestLoadPlugin {
    public static void main(String[] args) {
        try {
            java.io.File f = new java.io.File("dist/DPWValidationTool.jar");
            System.out.println("Testing plugin file: " + f.getAbsolutePath());
            PluginInformation info = new PluginInformation(f);
            System.out.println("PluginInformation created successfully.");
            System.out.println("Name: " + info.getName());
            System.out.println("Version: " + info.version);
            System.out.println("ClassName: " + info.className);
            System.out.println("Attributes: " + info.attr);
            try {
                Class<?> c = info.loadClass(TestLoadPlugin.class.getClassLoader());
                System.out.println("Loaded class: " + c.getName());
            } catch (Exception e) {
                System.out.println("Error loading class:");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("PluginInformation constructor failed:");
            e.printStackTrace();
        }
    }
}
