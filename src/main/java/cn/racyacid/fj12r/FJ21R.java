package cn.racyacid.fj12r;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import javax.swing.JOptionPane;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class FJ21R implements ModInitializer {
	private static final Logger LOGGER = Logger.getLogger("ForceJava21 Reborn");

	@Override
	public void onInitialize() {
		Path configPath = FabricLoader.getInstance().getConfigDir();
		String configFilePath = configPath.toString() + "\\fj21r.properties";

		// noinspection DataFlowIssue
		if (!Arrays.asList(configPath.toFile().list()).contains("fj21r.properties")) genConfigFile(configFilePath);

		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(configFilePath)) {
			properties.load(inputStream);
		} catch (IOException ioe) {
			LOGGER.severe("Failed to load config file, cause: " + ioe.getLocalizedMessage());
		}

		if (!isAllowedJavaVersionAndArch(properties)) {
			String crashInfo = genCrashInfo(properties);

			Object[] opinions = {"OK", "Copy and close"};
			if (JOptionPane.showOptionDialog(null, crashInfo, "WRONG JAVA VERSION OR ARCH!", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, opinions, opinions) == JOptionPane.NO_OPTION)
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(crashInfo), null);

			throw new RuntimeException("\n" + crashInfo);
		}

		LOGGER.info("Java version and arch check passed!");
	}

	private static String genCrashInfo(Properties properties) {
		String needsJavaVersion = properties.get("java_version").toString();
		String needsJavaArch = properties.get("java_arch").toString();

		return String.format("""
          Please use Java%s%s x%s to launch Minecraft!

          Current Java version: %s x%s

          Recommendations:
          - Amazon Corretto: %s
          - Azul Zulu: %s
          - Azul Platform Prime(Linux only): %s
          - Adoptium Eclipse Temurin: %s
        """,
		isEmpty(needsJavaVersion) ? "'s [Any version]" : needsJavaVersion,
		Boolean.parseBoolean(properties.get("enable_strict_check").toString()) ? "" : "(or later)",
		isEmpty(needsJavaArch) ? "[Any architecture]" : needsJavaArch,
		System.getProperty("java.specification.version"),
		System.getProperty("sun.arch.data.model"),
		properties.get("amz_link"),
		properties.get("zulu_link"),
		properties.get("platform_prime_link"),
		properties.get("temurin_link"));
	}

	private static void genConfigFile(String configFilePath) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath))) {
			writer.write("#ForceJava21 Reborn Config\n\n");

			writer.write("#Must be a specification Java version number!\n");
			writer.write("#Java1 ~ Java9: 1.0 ~ 1.9, Java10 and later: 10, 11, 12, ...\n");
			writer.write("#For example: Java8: 1.8, Java 11: 11, Java 21: 21\n");
			writer.write("#No restrict if blank\n");
			writer.write("java_version=\n\n");

			writer.write("#Can only be 32 or 64\n");
			writer.write("#No restrict if blank\n");
			writer.write("java_arch=\n\n");

			writer.write("amz_link=https://aws.amazon.com/corretto/\n");
			writer.write("zulu_link=https://www.azul.com/downloads/?package=jdk#zulu\n");
			writer.write("platform_prime_link=https://www.azul.com/downloads/?package=jdk#prime\n");
			writer.write("temurin_link=https://adoptium.net/temurin/releases/?package=jdk\n\n");

			writer.write("#If enable(true), game running Java version must equal to specified version\n");
			writer.write("#If not(false), game running Java version can be equal or more than the specified version\n");
			writer.write("#Cannot be blank!\n");
			writer.write("enable_strict_check=true");

			LOGGER.info("Created config file at " + configFilePath);
		} catch (IOException ioe) {
            LOGGER.severe("Failed to create config file, cause: " + ioe.getLocalizedMessage());
		}
	}

	private static boolean isAllowedJavaVersionAndArch(Properties properties) {
		float javaVersion = Float.parseFloat(System.getProperty("java.specification.version"));
		int javaArch = Integer.parseInt(System.getProperty("sun.arch.data.model"));
		String needsJavaVersion = properties.get("java_version").toString();
		String needsJavaArch = properties.get("java_arch").toString();

		boolean strictCheck = Boolean.parseBoolean(properties.get("enable_strict_check").toString());

		boolean versionCheck = isVersionValid(needsJavaVersion, javaVersion, strictCheck);
		boolean archCheck = isArchValid(needsJavaArch, javaArch, strictCheck);

		return versionCheck && archCheck;
	}

	private static boolean isVersionValid(String needsJavaVersion, float javaVersion, boolean strictCheck) {
		if (isEmpty(needsJavaVersion)) return true;
		return strictCheck ? javaVersion == Float.parseFloat(needsJavaVersion) : javaVersion >= Float.parseFloat(needsJavaVersion);
	}

	private static boolean isArchValid(String needsJavaArch, int javaArch, boolean strictCheck) {
		if (isEmpty(needsJavaArch)) return true;
		return strictCheck ? javaArch == Integer.parseInt(needsJavaArch) : javaArch >= Integer.parseInt(needsJavaArch);
	}

	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}
}