package net.mms_projects.copy_it.server;

import jlibs.core.util.regex.TemplateMatcher;
import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.config.MissingKey;

import java.io.*;
import java.util.Locale;

public class PageGenerator {

    public static String MESSAGE_PREFIX = "messages.";

    public static Locale[] locales = {
        new Locale("nl", "nl"), new Locale("de", "de")
    };

    public static void generate() {
        for (Locale locale : locales) {
            generatePage("authorize", locale);
        }
    }

    public static void generatePage(String page, final Locale locale) {
        String html = null;
        try {
            html = FileCache.get(page + ".html");
        } catch (IOException e) {
            Messages.printError("Could not read page file: " + e.getMessage());
        } catch (MissingKey missingKey) {
            // Should not happen
        }
        TemplateMatcher matcher = new TemplateMatcher("${", "}");
        String result = matcher.replace(html, new TemplateMatcher.VariableResolver() {
            @Override
            public String resolve(String variable) {
                if (variable.startsWith(MESSAGE_PREFIX)) {
                    String key = variable.substring(MESSAGE_PREFIX.length());
                    String result = Messages.getString(key, locale);
                    if (result == null) {
                        return "!" + key + "!";
                    } else {
                        return result;
                    }
                }

                return "${" + variable + "}";
            }
        });

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(Config.getString(Config.Keys.HTTP_FILES) + "/" + page + ".html." + locale.getLanguage() + "_" + locale.getCountry(), "UTF-8");
        } catch (Exception e) {
            Messages.printError("Could not write language file: " + e.getMessage());
        }
        writer.print(result);
        writer.close();

        Messages.printOK("Created " + locale.getLanguage() + "_" + locale.getCountry()
                + " locale for page \"" + page + "\"");
    }

}
