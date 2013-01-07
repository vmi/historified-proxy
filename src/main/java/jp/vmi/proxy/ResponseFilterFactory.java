package jp.vmi.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.littleshoot.proxy.HttpFilter;
import org.littleshoot.proxy.HttpResponseFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseFilterFactory implements HttpResponseFilters {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ResponseFilterFactory.class);

    private final List<Entry<Pattern, ResponseFilter>> factories = new ArrayList<>();

    public ResponseFilterFactory(Conf conf) {
        for (List<Object> entry : conf.entries) {
            String hostPattern = (String) entry.get(0);
            if ("DEFAULT".equals(hostPattern))
                hostPattern = "";
            ResponseFilter factory = new ResponseFilter();
            int cnt = entry.size();
            for (int i = 1; i < cnt; i++) {
                @SuppressWarnings("unchecked")
                List<String> pathEntry = (List<String>) entry.get(i);
                PathMatcher pathMatcher = new PathMatcher(pathEntry);
                factory.addPathMatcher(pathMatcher);
            }
            registerFactory(hostPattern, factory);
        }
    }

    private void registerFactory(final String hostPattern, final ResponseFilter filter) {
        Entry<Pattern, ResponseFilter> entry = new Entry<Pattern, ResponseFilter>() {
            private final Pattern key = Pattern.compile(hostPattern);
            private final ResponseFilter value = filter;

            @Override
            public Pattern getKey() {
                return key;
            }

            @Override
            public ResponseFilter getValue() {
                return value;
            }

            @Override
            public ResponseFilter setValue(ResponseFilter paramV) {
                throw new UnsupportedOperationException("setValue");
            }
        };
        factories.add(entry);
    }

    @Override
    public HttpFilter getFilter(String hostAndPort) {
        for (Entry<Pattern, ResponseFilter> entry : factories) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(hostAndPort);
            if (matcher.find())
                return entry.getValue();
        }
        return null;
    }

}
