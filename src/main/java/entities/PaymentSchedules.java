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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "payment_schedules")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentSchedules.findAll", query = "SELECT p FROM PaymentSchedules p"),
    @NamedQuery(name = "PaymentSchedules.findByReference", query = "SELECT p FROM PaymentSchedules p WHERE p.reference = :reference"),
    @NamedQuery(name = "PaymentSchedules.findByTotal", query = "SELECT p FROM PaymentSchedules p WHERE p.total = :total"),
    @NamedQuery(name = "PaymentSchedules.findByConfirmationStatus", query = "SELECT p FROM PaymentSchedules p WHERE p.confirmationStatus = :confirmationStatus"),
    @NamedQuery(name = "PaymentSchedules.findByCreatedAt", query = "SELECT p FROM PaymentSchedules p WHERE p.createdAt = :createdAt")})
public class PaymentSchedules implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "reference")
    private String reference;
    @Basic(optional = false)
    @NotNull
    @Column(name = "total")
    private long total;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 24)
    @Column(name = "confirmation_status")
    private String confirmationStatus;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @OneToOne(mappedBy = "scheduleId")
    private PaymentRequests paymentRequests;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "scheduleId")
    private List<Installments> installmentsList;
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Clients clientId;
    @JoinColumn(name = "manager_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Users managerId;

    public PaymentSchedules() {
    }

    public PaymentSchedules(UUID id) {
        this.id = id;
    }

    public PaymentSchedules(UUID id, String reference, long total, String confirmationStatus, Date createdAt) {
        this.id = id;
        this.reference = reference;
        this.total = total;
        this.confirmationStatus = confirmationStatus;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getConfirmationStatus() {
        return confirmationStatus;
    }

    public void setConfirmationStatus(String confirmationStatus) {
        this.confirmationStatus = confirmationStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentRequests getPaymentRequests() {
        return paymentRequests;
    }

    public void setPaymentRequests(PaymentRequests paymentRequests) {
        this.paymentRequests = paymentRequests;
    }

    @XmlTransient
    public List<Installments> getInstallmentsList() {
        return installmentsList;
    }

    public void setInstallmentsList(List<Installments> installmentsList) {
        this.installmentsList = installmentsList;
    }

    public Clients getClientId() {
        return clientId;
    }

    public void setClientId(Clients clientId) {
        this.clientId = clientId;
    }

    public Users getManagerId() {
        return managerId;
    }

    public void setManagerId(Users managerId) {
        this.managerId = managerId;
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
        if (!(object instanceof PaymentSchedules)) {
            return false;
        }
        PaymentSchedules other = (PaymentSchedules) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.PaymentSchedules[ id=" + id + " ]";
    }
    
}
