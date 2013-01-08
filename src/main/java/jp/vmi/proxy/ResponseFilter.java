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

public class ResponseFilter implements HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(ResponseFilter.class);

    private static final int OK = HttpResponseStatus.OK.getCode();

    private final Historifier historifier = new Historifier();
    private final List<PathMatcher> pathMatchers = new ArrayList<>();

    public void addPathMatcher(PathMatcher pathMatcher) {
        pathMatchers.add(pathMatcher);
    }

    private static class PathMatcherResult {

        public final String canonPath;
        public final boolean isInclude;

        public PathMatcherResult(String canonPath, boolean isInclude) {
            super();
            this.canonPath = canonPath;
            this.isInclude = isInclude;
        }
    }

    private PathMatcherResult matchPath(String uri) {
        String path = URIUtil.getFromPath(uri);
        for (PathMatcher pathMatcher : pathMatchers) {
            String canonPath = pathMatcher.canonicalize(path);
            if (canonPath != null)
                return new PathMatcherResult(canonPath, pathMatcher.isInclude());
        }
        return null;
    }

    @Override
    public boolean filterResponses(HttpRequest request) {
        if (request.getMethod() == HttpMethod.GET) {
            String uri = request.getUri();
            PathMatcherResult result = matchPath(uri);
            if (result != null) {
                if (result.isInclude)
                    log.info("Include: [{}]", uri);
                else
                    log.info("Exclude: [{}]", uri);
                return result.isInclude;
            }
            log.info("Skip: {}", uri);
        }
        return false;
    }

    @Override
    public HttpResponse filterResponse(HttpRequest request, HttpResponse response) {
        String uri = request.getUri();
        HttpResponseStatus status = response.getStatus();
        if (status.getCode() == OK) {
            PathMatcherResult result = matchPath(uri);
            String host = URI.create(uri).getHost();
            String key = host + result.canonPath;
            historifier.storeResponse(key, host, uri, response);
        } else {
            log.info("Skip: Status=[{}], URI=[{}]", status, uri);
        }
        return response;
    }

    @Override
    public int getMaxResponseSize() {
        return 100 * 1024 * 1024; // 100MB
    }
}
