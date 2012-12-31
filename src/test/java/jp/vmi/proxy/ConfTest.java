package jp.vmi.proxy;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;

import org.junit.Test;

public class ConfTest {

    @Test
    public void test() throws Exception {
        Conf conf = Conf.load(Conf.class.getResource("/proxy-test.conf"));
        assertThat(conf.storage.path, equalTo("${user.dir}/tmp/storage"));
        assertThat(conf.storage.user, equalTo("contents-user"));
        assertThat(conf.storage.password, equalTo("contents-password"));
        assertThat(conf.entries, is(Arrays.asList(
            Arrays.asList("google.com"),
            Arrays.asList("blogs.yahoo.co.jp", "^/\\w+/\\w+.html$"),
            Arrays.asList("www.amazon.co.jp", "(/dp/\\w+)/.*", "$1")
            )));
    }
}
