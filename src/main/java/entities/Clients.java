/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "clients")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Clients.findAll", query = "SELECT c FROM Clients c"),
    @NamedQuery(name = "Clients.findByName", query = "SELECT c FROM Clients c WHERE c.name = :name"),
    @NamedQuery(name = "Clients.findByEmail", query = "SELECT c FROM Clients c WHERE c.email = :email"),
    @NamedQuery(name = "Clients.findByPhone", query = "SELECT c FROM Clients c WHERE c.phone = :phone"),
    @NamedQuery(name = "Clients.findBySmsRemindersEnabled", query = "SELECT c FROM Clients c WHERE c.smsRemindersEnabled = :smsRemindersEnabled"),
    @NamedQuery(name = "Clients.findByCreatedAt", query = "SELECT c FROM Clients c WHERE c.createdAt = :createdAt"),
    @NamedQuery(name = "Clients.findByEmailRemindersEnabled", query = "SELECT c FROM Clients c WHERE c.emailRemindersEnabled = :emailRemindersEnabled")})
public class Clients implements Serializable {

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
    @Column(name = "sms_reminders_enabled")
    private boolean smsRemindersEnabled;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "email_reminders_enabled")
    private boolean emailRemindersEnabled;
    @Basic(optional = false)
    @NotNull
    @Column(name = "credit_balance")
    private long creditBalance;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "clientId")
    private List<PaymentRequests> paymentRequestsList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "clientId")
    private List<PaymentSchedules> paymentSchedulesList;
    @OneToMany(mappedBy = "clientId")
    private List<Users> usersList;

    public Clients() {
    }

    public Clients(UUID id) {
        this.id = id;
    }

    public Clients(UUID id, String name, String email, boolean smsRemindersEnabled, Date createdAt, boolean emailRemindersEnabled) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.smsRemindersEnabled = smsRemindersEnabled;
        this.createdAt = createdAt;
        this.emailRemindersEnabled = emailRemindersEnabled;
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

    public boolean getSmsRemindersEnabled() {
        return smsRemindersEnabled;
    }

    public void setSmsRemindersEnabled(boolean smsRemindersEnabled) {
        this.smsRemindersEnabled = smsRemindersEnabled;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getEmailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public void setEmailRemindersEnabled(boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }

    public long getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(long creditBalance) {
        this.creditBalance = creditBalance;
    }

    @XmlTransient
    public List<PaymentRequests> getPaymentRequestsList() {
        return paymentRequestsList;
    }

    public void setPaymentRequestsList(List<PaymentRequests> paymentRequestsList) {
        this.paymentRequestsList = paymentRequestsList;
    }

    @XmlTransient
    public List<PaymentSchedules> getPaymentSchedulesList() {
        return paymentSchedulesList;
    }

    public void setPaymentSchedulesList(List<PaymentSchedules> paymentSchedulesList) {
        this.paymentSchedulesList = paymentSchedulesList;
    }

    @XmlTransient
    public List<Users> getUsersList() {
        return usersList;
    }

    public void setUsersList(List<Users> usersList) {
        this.usersList = usersList;
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
        if (!(object instanceof Clients)) {
            return false;
        }
        Clients other = (Clients) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.Clients[ id=" + id + " ]";
    }
    
}
