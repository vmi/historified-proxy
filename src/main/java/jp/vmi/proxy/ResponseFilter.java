package jp.vmi.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.littleshoot.proxy.HttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseFilter implements HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(ResponseFilter.class);

    private static final int OK = HttpResponseStatus.OK.getCode();

    public static Map<String, HttpFilter> setupFilterMap(Conf conf) {
        log.info("Setup filters.");
        Map<String, HttpFilter> filters = new HashMap<>();
        for (List<String> entry : conf.entries) {
            String host;
            String regex = null;
            String replacement = null;
            switch (entry.size()) {
            case 3:
                replacement = entry.get(2);
                // fall through
            case 2:
                regex = entry.get(1);
                // fall through
            case 1:
                host = entry.get(0);
                break;
            default:
                throw new RuntimeException();
            }
            ResponseFilter filter = (ResponseFilter) filters.get(host);
            if (filter == null) {
                filter = new ResponseFilter(host);
                filters.put(host, filter);
            }
            URICanonicalizer canon = new URICanonicalizer(regex, replacement);
            filter.addCanonicalizer(canon);
            log.info("host=[{}], regex=[{}], replacement=[{}]", host, regex, replacement);
        }
        return filters;
    }

    private final Historifier historifier = new Historifier();
    private final List<URICanonicalizer> canonList = new ArrayList<>();
    private final String host;
    private String uri;
    private String key;

    public ResponseFilter(String host) {
        this.host = host;
    }

    public void addCanonicalizer(URICanonicalizer canon) {
        canonList.add(canon);
    }

    @Override
    public boolean shouldFilterResponses(HttpRequest request) {
        if (request.getMethod() == HttpMethod.GET) {
            uri = request.getUri();
            for (URICanonicalizer canon : canonList) {
                if ((key = canon.canonicalize(uri)) != null)
                    return true;
            }
        }
        return false;
    }

    private void saveContent(HttpResponse response) {
        historifier.storeResponse(key, host, uri, response);
    }

    @Override
    public HttpResponse filterResponse(HttpResponse response) {
        HttpResponseStatus status = response.getStatus();
        if (status.getCode() == OK)
            saveContent(response);
        return response;
    }

    @Override
    public int getMaxResponseSize() {
        return 100 * 1024 * 1024; // 100MB
    }
}
