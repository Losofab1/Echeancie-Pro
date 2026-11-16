/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author fabri
 */
@Entity
@Table(name = "email_outbox")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EmailOutbox.findAll", query = "SELECT e FROM EmailOutbox e"),
    @NamedQuery(name = "EmailOutbox.findByEventKey", query = "SELECT e FROM EmailOutbox e WHERE e.eventKey = :eventKey"),
    @NamedQuery(name = "EmailOutbox.findByRecipient", query = "SELECT e FROM EmailOutbox e WHERE e.recipient = :recipient"),
    @NamedQuery(name = "EmailOutbox.findBySubject", query = "SELECT e FROM EmailOutbox e WHERE e.subject = :subject"),
    @NamedQuery(name = "EmailOutbox.findByBody", query = "SELECT e FROM EmailOutbox e WHERE e.body = :body"),
    @NamedQuery(name = "EmailOutbox.findByStatus", query = "SELECT e FROM EmailOutbox e WHERE e.status = :status"),
    @NamedQuery(name = "EmailOutbox.findByAttemptCount", query = "SELECT e FROM EmailOutbox e WHERE e.attemptCount = :attemptCount"),
    @NamedQuery(name = "EmailOutbox.findByNextAttemptAt", query = "SELECT e FROM EmailOutbox e WHERE e.nextAttemptAt = :nextAttemptAt"),
    @NamedQuery(name = "EmailOutbox.findByClaimedAt", query = "SELECT e FROM EmailOutbox e WHERE e.claimedAt = :claimedAt"),
    @NamedQuery(name = "EmailOutbox.findByCreatedAt", query = "SELECT e FROM EmailOutbox e WHERE e.createdAt = :createdAt"),
    @NamedQuery(name = "EmailOutbox.findBySentAt", query = "SELECT e FROM EmailOutbox e WHERE e.sentAt = :sentAt"),
    @NamedQuery(name = "EmailOutbox.findByLastError", query = "SELECT e FROM EmailOutbox e WHERE e.lastError = :lastError")})
public class EmailOutbox implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "event_key")
    private String eventKey;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 254)
    @Column(name = "recipient")
    private String recipient;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "subject")
    private String subject;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "body")
    private String body;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "attempt_count")
    private int attemptCount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "next_attempt_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextAttemptAt;
    @Column(name = "claimed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date claimedAt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;
    @Size(max = 1000)
    @Column(name = "last_error")
    private String lastError;

    public EmailOutbox() {
    }

    public EmailOutbox(UUID id) {
        this.id = id;
    }

    public EmailOutbox(UUID id, String eventKey, String recipient, String subject, String body, String status, int attemptCount, Date nextAttemptAt, Date createdAt) {
        this.id = id;
        this.eventKey = eventKey;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Date getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Date nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Date getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Date claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EmailOutbox)) {
            return false;
        }
        EmailOutbox other = (EmailOutbox) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.EmailOutbox[ id=" + id + " ]";
    }
    
}
