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
@Table(name = "payment_requests")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentRequests.findAll", query = "SELECT p FROM PaymentRequests p"),
    @NamedQuery(name = "PaymentRequests.findByTotal", query = "SELECT p FROM PaymentRequests p WHERE p.total = :total"),
    @NamedQuery(name = "PaymentRequests.findByInstallmentCount", query = "SELECT p FROM PaymentRequests p WHERE p.installmentCount = :installmentCount"),
    @NamedQuery(name = "PaymentRequests.findByStatus", query = "SELECT p FROM PaymentRequests p WHERE p.status = :status"),
    @NamedQuery(name = "PaymentRequests.findByNote", query = "SELECT p FROM PaymentRequests p WHERE p.note = :note"),
    @NamedQuery(name = "PaymentRequests.findByChannel", query = "SELECT p FROM PaymentRequests p WHERE p.channel = :channel"),
    @NamedQuery(name = "PaymentRequests.findByCreatedAt", query = "SELECT p FROM PaymentRequests p WHERE p.createdAt = :createdAt"),
    @NamedQuery(name = "PaymentRequests.findByReviewedAt", query = "SELECT p FROM PaymentRequests p WHERE p.reviewedAt = :reviewedAt"),
    @NamedQuery(name = "PaymentRequests.findByAgreedTotal", query = "SELECT p FROM PaymentRequests p WHERE p.agreedTotal = :agreedTotal")})
public class PaymentRequests implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "total")
    private long total;
    @Basic(optional = false)
    @NotNull
    @Column(name = "installment_count")
    private short installmentCount;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status")
    private String status;
    @Size(max = 300)
    @Column(name = "note")
    private String note;
    @Size(max = 32)
    @Column(name = "channel")
    private String channel;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;
    @Column(name = "agreed_total")
    private Long agreedTotal;
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Clients clientId;
    @JoinColumn(name = "schedule_id", referencedColumnName = "id")
    @OneToOne
    private PaymentSchedules scheduleId;
    @JoinColumn(name = "requester_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Users requesterId;
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    @ManyToOne
    private Users reviewedBy;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "requestId")
    private List<PaymentRequestInstallments> paymentRequestInstallmentsList;

    public PaymentRequests() {
    }

    public PaymentRequests(UUID id) {
        this.id = id;
    }

    public PaymentRequests(UUID id, long total, short installmentCount, String status, Date createdAt) {
        this.id = id;
        this.total = total;
        this.installmentCount = installmentCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public short getInstallmentCount() {
        return installmentCount;
    }

    public void setInstallmentCount(short installmentCount) {
        this.installmentCount = installmentCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public Long getAgreedTotal() {
        return agreedTotal;
    }

    public void setAgreedTotal(Long agreedTotal) {
        this.agreedTotal = agreedTotal;
    }

    public Clients getClientId() {
        return clientId;
    }

    public void setClientId(Clients clientId) {
        this.clientId = clientId;
    }

    public PaymentSchedules getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(PaymentSchedules scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Users getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Users requesterId) {
        this.requesterId = requesterId;
    }

    public Users getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Users reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    @XmlTransient
    public List<PaymentRequestInstallments> getPaymentRequestInstallmentsList() {
        return paymentRequestInstallmentsList;
    }

    public void setPaymentRequestInstallmentsList(List<PaymentRequestInstallments> paymentRequestInstallmentsList) {
        this.paymentRequestInstallmentsList = paymentRequestInstallmentsList;
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
        if (!(object instanceof PaymentRequests)) {
            return false;
        }
        PaymentRequests other = (PaymentRequests) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.PaymentRequests[ id=" + id + " ]";
    }
    
}
