package jp.vmi.proxy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class URICanonicalizer {

    private static final Pattern URI_SPLITTER = Pattern.compile("(\\w+:/+[^/]+)/*(.*)");

    private final Pattern pattern;
    private final String replacement;

    public URICanonicalizer(String regex, String replacement) {
        this.pattern = StringUtils.isNotEmpty(regex) ? Pattern.compile(regex) : null;
        this.replacement = StringUtils.isNotEmpty(replacement) ? replacement : null;
    }

    public String canonicalize(String uri) {
        if (pattern == null)
            return uri;
        Matcher m1 = URI_SPLITTER.matcher(uri);
        String schemeAndHost = m1.group(1);
        String path = "/" + m1.group(2);
        Matcher m2 = pattern.matcher(path);
        if (!m2.find())
            return null;
        return replacement != null ? schemeAndHost + m2.replaceFirst(replacement) : uri;
    }
}
