package jp.vmi.proxy;

import java.io.File;

import jp.vmi.proxy.metadata.Metadata;

import org.littleshoot.proxy.DefaultHttpProxyServer;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpResponseFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Historified Proxy.
 */
public class Main implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final String[] args;

    public Main(String[] args) {
        this.args = args;
    }

    @Override
    public void run() {
        Conf conf;
        log.info("Start.");
        if (args.length > 0) {
            conf = Conf.load(new File(args[0]));
            log.info("Load configuration from: {}", args[0]);
        } else {
            conf = Conf.load(Conf.class.getResource("/proxy.conf"));
            log.info("Load default configuration.");
        }
        Historifier.initialize(conf);
        Metadata.initialize(conf);
        HttpResponseFilters factory = new ResponseFilterFactory(conf);
        HttpProxyServer server = new DefaultHttpProxyServer(8080, factory);
        server.start(true, true);
    }

    public static void main(String[] args) {
        new Main(args).run();
    }
}
