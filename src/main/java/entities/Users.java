/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author fabri
 */
@Entity
@Table(name = "users")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Users.findAll", query = "SELECT u FROM Users u"),
    @NamedQuery(name = "Users.findByName", query = "SELECT u FROM Users u WHERE u.name = :name"),
    @NamedQuery(name = "Users.findByEmail", query = "SELECT u FROM Users u WHERE u.email = :email"),
    @NamedQuery(name = "Users.findByPasswordHash", query = "SELECT u FROM Users u WHERE u.passwordHash = :passwordHash"),
    @NamedQuery(name = "Users.findByRole", query = "SELECT u FROM Users u WHERE u.role = :role"),
    @NamedQuery(name = "Users.findByActive", query = "SELECT u FROM Users u WHERE u.active = :active"),
    @NamedQuery(name = "Users.findByCreatedAt", query = "SELECT u FROM Users u WHERE u.createdAt = :createdAt")})
public class Users implements Serializable {

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
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "password_hash")
    private String passwordHash;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "role")
    private String role;
    @Basic(optional = false)
    @NotNull
    @Column(name = "active")
    private boolean active;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @OneToMany(mappedBy = "createdBy")
    private List<ClientCreationRequests> clientCreationRequestsList;
    @OneToMany(mappedBy = "reviewedBy")
    private List<ClientCreationRequests> clientCreationRequestsList1;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "requesterId")
    private List<PaymentRequests> paymentRequestsList;
    @OneToMany(mappedBy = "reviewedBy")
    private List<PaymentRequests> paymentRequestsList1;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "managerId")
    private List<PaymentSchedules> paymentSchedulesList;
    @OneToMany(mappedBy = "reviewedBy")
    private List<PaymentSubmissions> paymentSubmissionsList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "submittedBy")
    private List<PaymentSubmissions> paymentSubmissionsList1;
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    @ManyToOne
    private Clients clientId;

    public Users() {
    }

    public Users(UUID id) {
        this.id = id;
    }

    public Users(UUID id, String name, String email, String passwordHash, String role, boolean active, Date createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @XmlTransient
    public List<ClientCreationRequests> getClientCreationRequestsList() {
        return clientCreationRequestsList;
    }

    public void setClientCreationRequestsList(List<ClientCreationRequests> clientCreationRequestsList) {
        this.clientCreationRequestsList = clientCreationRequestsList;
    }

    @XmlTransient
    public List<ClientCreationRequests> getClientCreationRequestsList1() {
        return clientCreationRequestsList1;
    }

    public void setClientCreationRequestsList1(List<ClientCreationRequests> clientCreationRequestsList1) {
        this.clientCreationRequestsList1 = clientCreationRequestsList1;
    }

    @XmlTransient
    public List<PaymentRequests> getPaymentRequestsList() {
        return paymentRequestsList;
    }

    public void setPaymentRequestsList(List<PaymentRequests> paymentRequestsList) {
        this.paymentRequestsList = paymentRequestsList;
    }

    @XmlTransient
    public List<PaymentRequests> getPaymentRequestsList1() {
        return paymentRequestsList1;
    }

    public void setPaymentRequestsList1(List<PaymentRequests> paymentRequestsList1) {
        this.paymentRequestsList1 = paymentRequestsList1;
    }

    @XmlTransient
    public List<PaymentSchedules> getPaymentSchedulesList() {
        return paymentSchedulesList;
    }

    public void setPaymentSchedulesList(List<PaymentSchedules> paymentSchedulesList) {
        this.paymentSchedulesList = paymentSchedulesList;
    }

    @XmlTransient
    public List<PaymentSubmissions> getPaymentSubmissionsList() {
        return paymentSubmissionsList;
    }

    public void setPaymentSubmissionsList(List<PaymentSubmissions> paymentSubmissionsList) {
        this.paymentSubmissionsList = paymentSubmissionsList;
    }

    @XmlTransient
    public List<PaymentSubmissions> getPaymentSubmissionsList1() {
        return paymentSubmissionsList1;
    }

    public void setPaymentSubmissionsList1(List<PaymentSubmissions> paymentSubmissionsList1) {
        this.paymentSubmissionsList1 = paymentSubmissionsList1;
    }

    public Clients getClientId() {
        return clientId;
    }

    public void setClientId(Clients clientId) {
        this.clientId = clientId;
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
        if (!(object instanceof Users)) {
            return false;
        }
        Users other = (Users) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.Users[ id=" + id + " ]";
    }
    
}
