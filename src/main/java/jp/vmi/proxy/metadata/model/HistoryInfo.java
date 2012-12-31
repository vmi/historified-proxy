package jp.vmi.proxy.metadata.model;

import java.io.Serializable;
import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.eclipse.persistence.annotations.Index;

/**
 * Entity implementation class for Entity: Contents
 *
 */
@Entity
public class HistoryInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Index
    private String key;
    @Index
    private String host;
    @Index
    private String uri;
    private String contentType;
    private Long contentLength;
    private String title;
    private String checksum;
    private String path;
    private Date contentsCreated;
    private Date created;

    public HistoryInfo() {
    }

    public HistoryInfo(ContentInfo contents) {
        this.key = contents.getKey();
        this.host = contents.getHost();
        this.uri = contents.getUri();
        this.contentType = contents.getContentType();
        this.contentLength = contents.getContentLength();
        this.title = contents.getTitle();
        this.checksum = contents.getChecksum();
        this.path = contents.getPath();
        this.contentsCreated = contents.getCreated();
    }

    @PrePersist
    public void prePersist() {
        created = new Date(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getContentsCreated() {
        return contentsCreated;
    }

    public void setContentsCreated(Date contentsCreated) {
        this.contentsCreated = contentsCreated;
    }

}
