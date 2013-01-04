package jp.vmi.proxy;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathMatcher {

    private static enum Type {
        INCLUDE(true), EXCLUDE(false);

        public final boolean isInclude;

        Type(boolean isInclude) {
            this.isInclude = isInclude;
        }
    }

    private final Type type;

    private final Pattern pathPattern;

    private final String canonicalized;

    public PathMatcher(List<String> pathEntry) {
        String canonicalized = null;
        String pathPattern = null;
        switch (pathEntry.size()) {
        case 3:
            canonicalized = pathEntry.get(2);
            // fall through
        case 2:
            pathPattern = pathEntry.get(1);
            // fall through
        case 1:
            switch (pathEntry.get(0)) {
            case "include":
                this.type = Type.INCLUDE;
                break;
            case "exclude":
                this.type = Type.EXCLUDE;
                break;
            default:
                throw new IllegalArgumentException(pathEntry.get(0) + " is invalid parameter. Expected: include / exclude");
            }
            break;

        default:
            throw new IllegalArgumentException("invalid paramter count: " + pathEntry.toString());
        }
        if (pathPattern != null)
            this.pathPattern = Pattern.compile(pathPattern);
        else
            this.pathPattern = null;
        this.canonicalized = canonicalized;
    }

    public boolean isInclude() {
        return type.isInclude;
    }

    public String matches(String path) {
        if (pathPattern != null) {
            Matcher matcher = pathPattern.matcher(path);
            if (!matcher.find())
                return null;
            return matcher.replaceFirst(canonicalized);
        } else {
            return path;
        }
    }
}
