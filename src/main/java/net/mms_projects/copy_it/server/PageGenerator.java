package net.mms_projects.copy_it.server;

import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.config.MissingKey;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class PageGenerator {
    public static final String MESSAGE_PREFIX = "messages.";

    private static final Locale[] locales = {
        new Locale("nl", "nl"), new Locale("de", "de")
    };

    private static final Locale[] symlinks = {
        new Locale("nl", "nl"), new Locale("de", "de")
    };

    private static final String[] pages = { "authorize" };

    public static boolean generate() {
        if (!cleanUp())
            return false;
        for (String page : pages) {
            for (Locale locale : locales) {
                if (!generatePage(page, locale))
                    return false;
            }
            for (Locale locale : symlinks) {
                if (!createSymlink(page, locale))
                    return false;
            }
        }
        return true;
    }

    public static boolean checkPages() throws MissingKey {
        for (String page : pages) {
            File file = new File(Config.getString(Config.Keys.HTTP_FILES) + File.separator + page + DOT_HTML);
            final long lastUpdated = file.lastModified();
            for (Locale locale : locales) {
                File lpage = new File(Config.getString(Config.Keys.HTTP_FILES) + File.separator + page + DOT_HTML_DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry());
                if (lastUpdated > lpage.lastModified())
                    return true;
            }
            for (Locale locale : symlinks) {
                File lpage = new File(Config.getString(Config.Keys.HTTP_FILES) + File.separator + page + DOT_HTML_DOT + locale.getLanguage());
                if (lastUpdated > lpage.lastModified())
                    return true;
            }
        }
        return false;
    }

    private static boolean cleanUp() {
        try {
            File dir = new File(Config.getString(Config.Keys.HTTP_FILES));
            File[] files = dir.listFiles();
            for (File file : files) {
                for (Locale locale : locales) {
                    if (file.getName().endsWith(locale.getLanguage())
                        || file.getName().endsWith(locale.getLanguage() + UNDERSCORE + locale.getCountry())) {
                        file.delete();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static final String DOT_HTML = ".html";
    private static final String DOT_HTML_DOT = ".html.";
    private static final TemplateMatcher TEMPLATE_MATCHER = new TemplateMatcher("${", "}");
    private static final String UNDERSCORE = "_";

    private static boolean generatePage(String page, Locale locale) {
        String html = null;
        try {
            html = FileCache.get(page + DOT_HTML);
        } catch (Exception e) {
            Messages.printError("Could not read page file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        String result = TEMPLATE_MATCHER.replace(html, new TextResolver(locale));
        try {
            PrintWriter writer = new PrintWriter(Config.getString(Config.Keys.HTTP_FILES) + File.separator + page + DOT_HTML_DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry(), "UTF-8");
            writer.print(result);
            writer.close();
        } catch (Exception e) {
            Messages.printError("Could not write language file: " + e.getMessage());
            return false;
        }
        Messages.printOK("Created " + locale.getLanguage() + "_" + locale.getCountry()
                + " locale for page \"" + page + "\"");
        return true;
    }

    private static boolean createSymlink(String page, Locale locale) {
        try {
            Path link = Paths.get(page + DOT_HTML_DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry());
            Path dst = Paths.get(Config.getString(Config.Keys.HTTP_FILES) + File.separator + page + DOT_HTML_DOT + locale.getLanguage());
            Files.createSymbolicLink(dst, link);
        } catch (Exception e) {
            Messages.printError("Could not create symlink for " + page + DOT_HTML_DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry());
            e.printStackTrace();
            return false;
        }
        Messages.printOK("Created symlink for " + page + DOT_HTML_DOT + locale.getLanguage() + UNDERSCORE + locale.getCountry());
        return true;
    }

    private static final class TextResolver implements TemplateMatcher.VariableResolver {
        public TextResolver(Locale locale) {
            this.locale = locale;
        }

        public String resolve(String s) {
            if (s.startsWith(MESSAGE_PREFIX)) {
                String key = s.substring(MESSAGE_PREFIX.length());
                String result = Messages.getString(key, locale);
                if (result == null)
                    return "!" + key + "!";
                else
                    return result;
            } else if (Config.hasString(s))
                return Config.getStringSafe(s, null);
            return "${" + s + "}";
        }

        private final Locale locale;
    }
}
