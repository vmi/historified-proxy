package jp.vmi.proxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.util.URIUtil;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.littleshoot.proxy.HttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseFilterFactory {

    private static final Logger log = LoggerFactory.getLogger(ResponseFilterFactory.class);

    private static final int OK = HttpResponseStatus.OK.getCode();

    private final Historifier historifier = new Historifier();
    private final List<PathMatcher> pathMatchers = new ArrayList<>();

    public void addPathMatcher(PathMatcher pathMatcher) {
        pathMatchers.add(pathMatcher);
    }

    public HttpFilter newFilter() {
        return new ResponseFilter();
    }

    private final class ResponseFilter implements HttpFilter {

        private String key;
        private String host;
        private String uri;

        @Override
        public boolean shouldFilterResponses(HttpRequest request) {
            if (request.getMethod() == HttpMethod.GET) {
                uri = request.getUri();
                String path = URIUtil.getFromPath(uri);
                for (PathMatcher pathMatcher : pathMatchers) {
                    String canonPath = pathMatcher.matches(path);
                    if (canonPath != null) {
                        host = URI.create(uri).getHost();
                        key = host + canonPath;
                        boolean isInclude = pathMatcher.isInclude();
                        if (isInclude)
                            log.info("Include: [{}]", uri);
                        else
                            log.info("Exclude: [{}]", uri);
                        return isInclude;
                    }
                }
                log.info("Skip: {}", uri);
            }
            return false;
        }

        @Override
        public HttpResponse filterResponse(HttpResponse response) {
            HttpResponseStatus status = response.getStatus();
            if (status.getCode() == OK)
                historifier.storeResponse(key, host, uri, response);
            return response;
        }

        @Override
        public int getMaxResponseSize() {
            return 100 * 1024 * 1024; // 100MB
        }
    }
}
