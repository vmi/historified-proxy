package jp.vmi.proxy.metadata;

import java.io.File;
import java.util.List;

import jp.vmi.proxy.Conf;
import jp.vmi.proxy.metadata.model.ContentInfo;
import jp.vmi.proxy.metadata.model.HistoryInfo;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class MetadataTest {

    @Test
    public void testRegister() throws Exception {
        Conf conf = Conf.load(Conf.class.getResource("/proxy-test.conf"));
        File dir = new File(conf.storage.path);
        if (dir.isDirectory())
            FileUtils.deleteDirectory(dir);
        Metadata.initialize(conf);
        try (Metadata storage = new Metadata()) {
            String key = "http://localhost/";
            ContentInfo c1 = storage.search(key);
            assertThat(c1, is(nullValue()));
            c1 = new ContentInfo(key, "localhost", "http://localhost/");
            c1.setContentType("text/html");
            c1.setCharset("UTF-8");
            c1.setContentLength(128L);
            c1.setTitle("test title");
            storage.register(c1);
            ContentInfo c2 = storage.search(key);
            assertThat(c2, equalTo(c1));
            String updateTitle = "update title";
            c2.setTitle(updateTitle);
            storage.register(c2);
            ContentInfo c3 = storage.search(key);
            assertThat(c3.getTitle(), is(updateTitle));
            long count = storage.getHistoryCount(key);
            assertThat(count, is(2L));
            List<HistoryInfo> historyList = storage.getHistory(key);
            assertThat(historyList.size(), is(2));
            assertThat(historyList.get(0).getTitle(), is("test title"));
            assertThat(historyList.get(1).getTitle(), equalTo(updateTitle));
        }
    }
}
