package jp.vmi.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.text.StrSubstitutor;
import org.yaml.snakeyaml.Yaml;

public class Conf {

    public static class Storage {

        public String path;

        public String user;

        public String password;

        private void expandVars() {
            path = StrSubstitutor.replaceSystemProperties(path).replace('\\', '/');
            user = StrSubstitutor.replaceSystemProperties(user);
            password = StrSubstitutor.replaceSystemProperties(password);
        }
    }

    public Storage storage;

    public List<List<Object>> entries;

    public static Conf load(InputStream is) {
        try {
            Yaml yaml = new Yaml();
            Reader reader = new InputStreamReader(is, "UTF-8");
            Conf conf = yaml.loadAs(reader, Conf.class);
            conf.storage.expandVars();
            return conf;
        } catch (UnsupportedEncodingException e) {
            // not reached.
            throw new RuntimeException(e);
        }
    }

    public static Conf load(URL url) {
        try (InputStream is = url.openStream()) {
            return load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Conf load(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
