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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "client_creation_requests")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ClientCreationRequests.findAll", query = "SELECT c FROM ClientCreationRequests c"),
    @NamedQuery(name = "ClientCreationRequests.findByName", query = "SELECT c FROM ClientCreationRequests c WHERE c.name = :name"),
    @NamedQuery(name = "ClientCreationRequests.findByEmail", query = "SELECT c FROM ClientCreationRequests c WHERE c.email = :email"),
    @NamedQuery(name = "ClientCreationRequests.findByPhone", query = "SELECT c FROM ClientCreationRequests c WHERE c.phone = :phone"),
    @NamedQuery(name = "ClientCreationRequests.findByStatus", query = "SELECT c FROM ClientCreationRequests c WHERE c.status = :status"),
    @NamedQuery(name = "ClientCreationRequests.findByCreatedAt", query = "SELECT c FROM ClientCreationRequests c WHERE c.createdAt = :createdAt"),
    @NamedQuery(name = "ClientCreationRequests.findByReviewedAt", query = "SELECT c FROM ClientCreationRequests c WHERE c.reviewedAt = :reviewedAt"),
    @NamedQuery(name = "ClientCreationRequests.findByPasswordHash", query = "SELECT c FROM ClientCreationRequests c WHERE c.passwordHash = :passwordHash")})
public class ClientCreationRequests implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 120)
    @Column(name = "name")
    private String name;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 254)
    @Column(name = "email")
    private String email;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 32)
    @Column(name = "phone")
    private String phone;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;
    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ManyToOne
    private Users createdBy;
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    @ManyToOne
    private Users reviewedBy;

    public ClientCreationRequests() {
    }

    public ClientCreationRequests(UUID id) {
        this.id = id;
    }

    public ClientCreationRequests(UUID id, String name, String email, String status, Date createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Users getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Users createdBy) {
        this.createdBy = createdBy;
    }

    public Users getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Users reviewedBy) {
        this.reviewedBy = reviewedBy;
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
        if (!(object instanceof ClientCreationRequests)) {
            return false;
        }
        ClientCreationRequests other = (ClientCreationRequests) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.ClientCreationRequests[ id=" + id + " ]";
    }
    
}
