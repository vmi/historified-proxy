package jp.vmi.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.littleshoot.proxy.HttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterMap implements Map<String, HttpFilter> {

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
    public HttpFilter get(Object key) {
        for (Entry<Pattern, ResponseFilterFactory> entry : factories) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher((CharSequence) key);
            if (matcher.find())
                return entry.getValue().newFilter();
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return factories.isEmpty();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("containsKey");
    }

    @Override
    public boolean containsValue(Object arg0) {
        throw new UnsupportedOperationException("containsValue");
    }

    @Override
    public HttpFilter put(String arg0, HttpFilter arg1) {
        throw new UnsupportedOperationException("put");
    }

    @Override
    public Set<java.util.Map.Entry<String, HttpFilter>> entrySet() {
        throw new UnsupportedOperationException("entrySet");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("keySet");
    }

    @Override
    public void putAll(Map<? extends String, ? extends HttpFilter> arg0) {
        throw new UnsupportedOperationException("putAll");
    }

    @Override
    public HttpFilter remove(Object arg0) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("size");
    }

    @Override
    public Collection<HttpFilter> values() {
        throw new UnsupportedOperationException("values");
    }

}
