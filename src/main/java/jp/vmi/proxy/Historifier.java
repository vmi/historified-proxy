package jp.vmi.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import jp.vmi.proxy.metadata.Metadata;
import jp.vmi.proxy.metadata.model.ContentInfo;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.parsers.DOMParser;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import static org.apache.xerces.impl.Constants.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;

public class Historifier {

    private static final Logger log = LoggerFactory.getLogger(Historifier.class);

    private static final FastDateFormat PATH_BASE = FastDateFormat.getInstance("yyyy/MM/dd/HHmmss_SSS_");

    private static class ResponseInfo {

        private static final Pattern CHARSET = Pattern.compile("charset=\"?(.+)\"?", Pattern.CASE_INSENSITIVE);

        public final String contentType;
        public final String charset;
        public final long contentLength;
        public final String checksum;

        public ResponseInfo(HttpResponse response) {
            String ct = response.getHeader(CONTENT_TYPE);
            if (StringUtils.isBlank(ct)) {
                contentType = "application/octet-stream";
                charset = null;
            } else {
                String[] cts = ct.split("\\s*;\\s*");
                contentType = cts[0];
                String cs = null;
                for (int i = 1; i < cts.length; i++) {
                    Matcher matcher = CHARSET.matcher(cts[i]);
                    if (matcher.matches()) {
                        cs = matcher.group(1);
                        break;
                    }
                }
                charset = cs;
            }
            String cls = response.getHeader(CONTENT_LENGTH);
            long cl = -1;
            try {
                cl = Long.parseLong(cls);
            } catch (NumberFormatException e) {
                // no operation
            }
            contentLength = cl;
            MessageDigest md = DigestUtils.getSha1Digest();
            ByteBuffer buffer = response.getContent().toByteBuffer();
            md.update(buffer);
            checksum = Hex.encodeHexString(md.digest());
        }
    }

    private static File baseDir;

    public static void initialize(Conf conf) {
        baseDir = new File(conf.storage.path, "contents");
        if (!baseDir.isDirectory())
            baseDir.mkdirs();
    }

    private String generateSavePath(String uri, String mimeType) {
        // 1. get path from uri.
        // 2. add "index" if ends with "/".
        // 3. remove first "/".
        // 4. replace "/" to "@".
        // ex) http://example.com -> index
        //     http://example.com/user/info  -> user$info
        //     http://example.com/user/info/ -> user$info$index
        URI uriObj = URI.create(uri);
        String name = uriObj.getHost() + "@" + uriObj.getPath().replaceFirst("/+$", "/index").replaceFirst("^/+", "").replace('/', '@');
        if (mimeType.endsWith("/html")) {
            // all html files
            name = name.replaceFirst("(?:\\.\\w+)?$", ".html");
        } else if (!name.matches(".*\\.\\w+")) {
            switch (mimeType) {
            case "text/plain":
                name += ".txt";
                break;
            default:
                name += mimeType.replaceFirst("^[^/]+/(?:x-)?", ".");
                break;
            }
        }
        long time = System.currentTimeMillis();
        while (true) {
            String path = PATH_BASE.format(time) + name;
            File file = new File(baseDir, path);
            File dir = file.getParentFile();
            if (!dir.isDirectory())
                dir.mkdirs();
            try {
                if (file.createNewFile())
                    return path;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            time++;
        }
    }

    private void saveContent(File file, ChannelBuffer buffer) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            FileChannel fc = fos.getChannel();
            int size = buffer.readableBytes();
            ByteBuffer byteBuffer = buffer.toByteBuffer();
            int written = 0;
            while (written < size)
                written += fc.write(byteBuffer);
            fc.force(false);
            fc.close();
        }
    }

    private void saveContentInfo(File file, ContentInfo contentInfo) {
        File file2 = new File(file.getParent(), file.getName() + ".yaml");
        try (FileWriterWithEncoding ps = new FileWriterWithEncoding(file2, "UTF-8")) {
            Yaml yaml = new Yaml();
            yaml.dump(contentInfo, ps);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTitle(File file, String charset) {
        try (InputStream is = new FileInputStream(file)) {
            DOMParser dp = new DOMParser();
            dp.setEntityResolver(null);
            dp.setFeature("http://xml.org/sax/features/namespaces", false);
            dp.setFeature(XERCES_FEATURE_PREFIX + INCLUDE_COMMENTS_FEATURE, true);
            dp.parse(new InputSource(is));
            Document document = dp.getDocument();
            Node titleNode = XPathAPI.selectSingleNode(document, "/HTML/HEAD/TITLE");
            return titleNode != null ? titleNode.getTextContent() : null;
        } catch (SAXException | IOException | TransformerException e) {
            log.error("Failed html parse: {}", e);
            return null;
        }
    }

    public void storeResponse(String key, String host, String uri, HttpResponse response) {
        try (Metadata metadata = new Metadata()) {
            ResponseInfo responseInfo = new ResponseInfo(response);
            ContentInfo contentInfo = metadata.search(key);
            if (contentInfo == null) {
                contentInfo = new ContentInfo(key, host, uri);
                log.info("New: [{}]", uri);
            } else if (contentInfo.getContentLength() == responseInfo.contentLength
                && StringUtils.equals(contentInfo.getChecksum(), responseInfo.checksum)) {
                metadata.rollback();
                log.info("Exists: [{}]", uri);
                return;
            } else {
                log.info("Updated: [{}]", uri);
            }
            String path = generateSavePath(uri, responseInfo.contentType);
            File file = new File(baseDir, path);
            saveContent(file, response.getContent());
            String title = null;
            if (responseInfo.contentType.endsWith("/html"))
                title = getTitle(file, responseInfo.charset);
            if (title == null)
                title = URIUtil.getName(uri);
            contentInfo.setContentType(responseInfo.contentType);
            contentInfo.setCharset(responseInfo.charset);
            contentInfo.setContentLength(responseInfo.contentLength);
            contentInfo.setChecksum(responseInfo.checksum);
            contentInfo.setTitle(title);
            contentInfo.setPath(path);
            saveContentInfo(file, contentInfo);
            metadata.register(contentInfo);
        } catch (IOException e) {
            log.error("Failed to storeResponse: {}", e);
        }

    }
}
