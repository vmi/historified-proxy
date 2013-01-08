package jp.vmi.proxy.metadata;

import java.io.Closeable;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import jp.vmi.proxy.Conf;
import jp.vmi.proxy.metadata.model.ContentInfo;
import jp.vmi.proxy.metadata.model.HistoryInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metadata implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(Metadata.class);

    private static EntityManagerFactory emf = null;

    public static synchronized void initialize(Conf conf) {
        if (emf != null)
            return;
        String path = conf.storage.path;
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url", "jdbc:h2:" + path + "/contents-map");
        props.put("javax.persistence.jdbc.user", conf.storage.user);
        props.put("javax.persistence.jdbc.password", conf.storage.password);
        emf = Persistence.createEntityManagerFactory("proxy", props);

    }

    private final EntityManager em;
    private final EntityTransaction tx;

    public Metadata() {
        em = emf.createEntityManager();
        tx = em.getTransaction();
    }

    public ContentInfo search(String key) {
        if (!tx.isActive())
            tx.begin();
        TypedQuery<ContentInfo> query = em.createQuery("SELECT c FROM ContentInfo c WHERE c.key = :key",
            ContentInfo.class);
        query.setParameter("key", key);
        List<ContentInfo> result = query.getResultList();
        return result.size() > 0 ? result.get(0) : null;
    }

    public void register(ContentInfo contentInfo) {
        if (!tx.isActive())
            tx.begin();
        try {
            contentInfo.setCreated();
            if (contentInfo.getId() == null)
                em.persist(contentInfo);
            HistoryInfo historyInfo = new HistoryInfo(contentInfo);
            em.persist(historyInfo);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive())
                tx.rollback();
            throw e;
        }
    }

    public long getHistoryCount(String key) {
        TypedQuery<Long> query = em.createQuery("SELECT count(h.id) FROM HistorInfo h WHERE h.key = :key", Long.class);
        query.setParameter("key", key);
        return query.getSingleResult();
    }

    public List<HistoryInfo> getHistory(String key) {
        TypedQuery<HistoryInfo> query = em
            .createQuery("SELECT h FROM HistorInfo h WHERE h.key = :key ORDER BY h.id", HistoryInfo.class);
        query.setParameter("key", key);
        return query.getResultList();
    }

    public void commit() {
        if (tx.isActive())
            tx.commit();
    }

    public void rollback() {
        if (tx.isActive())
            tx.rollback();
    }

    @Override
    public void close() {
        if (tx.isActive()) {
            try {
                tx.rollback();
            } catch (RuntimeException e) {
                log.error("rollback: {}", e);
            }
        }
        if (em.isOpen()) {
            try {
                em.close();
            } catch (RuntimeException e) {
                log.error("close: {}", e);
            }
        }
    }
}
