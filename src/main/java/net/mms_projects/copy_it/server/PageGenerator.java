package net.mms_projects.copy_it.server;

import jlibs.core.util.regex.TemplateMatcher;
import net.mms_projects.copy_it.server.config.Config;

import java.io.PrintWriter;
import java.util.Locale;

public class PageGenerator {
    public static String MESSAGE_PREFIX = "messages.";

    public static Locale[] locales = {
        new Locale("nl", "nl"), new Locale("de", "de")
    };

    public static boolean generate() {
        for (Locale locale : locales) {
            if (!generatePage("authorize", locale))
                return false;
        }
        return true;
    }

    public static boolean generatePage(String page, Locale locale) {
        String html = null;
        try {
            html = FileCache.get(page + ".html");
        } catch (Exception e) {
            Messages.printError("Could not read page file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        TemplateMatcher matcher = new TemplateMatcher("${", "}");
        String result = matcher.replace(html, new TextResolver(locale));

        try {
            PrintWriter writer = new PrintWriter(Config.getString(Config.Keys.HTTP_FILES) + "/" + page + ".html." + locale.getLanguage() + "_" + locale.getCountry(), "UTF-8");
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
            }
            return "${" + s + "}";
        }

        private final Locale locale;
    }
}
