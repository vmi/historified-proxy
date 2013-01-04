package jp.vmi.proxy;

import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.Test;

import static java.util.Arrays.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ConfTest {

    @Test
    public void test() throws Exception {
        Conf conf = Conf.load(Conf.class.getResource("/proxy-test.conf"));
        assertThat(conf.storage.path,
            equalTo(StrSubstitutor.replaceSystemProperties("${user.dir}/tmp/storage-test").replace('\\', '/')));
        assertThat(conf.storage.user, equalTo("contents-user"));
        assertThat(conf.storage.password, equalTo("contents-password"));
        assertThat(conf.entries, is(asList(
            asList("\\bamazon\\b", asList("include", ".*(/dp/\\w+)/.*", "$1"), asList((Object) "exclude")),
            asList("blog", asList("exclude", "/$"), asList((Object) "include")),
            asList("DEFAULT", asList((Object) "exclude"))
            )));
    }
}
