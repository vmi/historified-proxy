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

public class FilterMap implements HttpResponseFilters {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(FilterMap.class);

    private final List<Entry<Pattern, ResponseFilterFactory>> factories = new ArrayList<>();

    public FilterMap(Conf conf) {
        for (List<Object> entry : conf.entries) {
            String hostPattern = (String) entry.get(0);
            if ("DEFAULT".equals(hostPattern))
                hostPattern = "";
            ResponseFilterFactory factory = new ResponseFilterFactory();
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

    private void registerFactory(final String hostPattern, final ResponseFilterFactory filter) {
        Entry<Pattern, ResponseFilterFactory> entry = new Entry<Pattern, ResponseFilterFactory>() {
            private final Pattern key = Pattern.compile(hostPattern);
            private final ResponseFilterFactory value = filter;

            @Override
            public Pattern getKey() {
                return key;
            }

            @Override
            public ResponseFilterFactory getValue() {
                return value;
            }

            @Override
            public ResponseFilterFactory setValue(ResponseFilterFactory paramV) {
                throw new UnsupportedOperationException("setValue");
            }
        };
        factories.add(entry);
    }

    @Override
    public HttpFilter getFilter(String hostAndPort) {
        for (Entry<Pattern, ResponseFilterFactory> entry : factories) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(hostAndPort);
            if (matcher.find())
                return entry.getValue();
        }
        return null;
    }

}
