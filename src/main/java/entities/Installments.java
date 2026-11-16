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
@Table(name = "installments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Installments.findAll", query = "SELECT i FROM Installments i"),
    @NamedQuery(name = "Installments.findByInstallmentNumber", query = "SELECT i FROM Installments i WHERE i.installmentNumber = :installmentNumber"),
    @NamedQuery(name = "Installments.findByAmount", query = "SELECT i FROM Installments i WHERE i.amount = :amount"),
    @NamedQuery(name = "Installments.findByDueDate", query = "SELECT i FROM Installments i WHERE i.dueDate = :dueDate"),
    @NamedQuery(name = "Installments.findByPaid", query = "SELECT i FROM Installments i WHERE i.paid = :paid"),
    @NamedQuery(name = "Installments.findByPaidAt", query = "SELECT i FROM Installments i WHERE i.paidAt = :paidAt"),
    @NamedQuery(name = "Installments.findByChannel", query = "SELECT i FROM Installments i WHERE i.channel = :channel"),
    @NamedQuery(name = "Installments.findByPaymentReference", query = "SELECT i FROM Installments i WHERE i.paymentReference = :paymentReference")})
public class Installments implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "installment_number")
    private short installmentNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "amount")
    private long amount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "due_date")
    @Temporal(TemporalType.DATE)
    private Date dueDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "paid")
    private boolean paid;
    @Column(name = "paid_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidAt;
    @Size(max = 32)
    @Column(name = "channel")
    private String channel;
    @Size(max = 80)
    @Column(name = "payment_reference")
    private String paymentReference;
    @JoinColumn(name = "schedule_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private PaymentSchedules scheduleId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "installmentId")
    private List<PaymentSubmissions> paymentSubmissionsList;

    public Installments() {
    }

    public Installments(UUID id) {
        this.id = id;
    }

    public Installments(UUID id, short installmentNumber, long amount, Date dueDate, boolean paid) {
        this.id = id;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = paid;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public short getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(short installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean getPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public PaymentSchedules getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(PaymentSchedules scheduleId) {
        this.scheduleId = scheduleId;
    }

    @XmlTransient
    public List<PaymentSubmissions> getPaymentSubmissionsList() {
        return paymentSubmissionsList;
    }

    public void setPaymentSubmissionsList(List<PaymentSubmissions> paymentSubmissionsList) {
        this.paymentSubmissionsList = paymentSubmissionsList;
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
        if (!(object instanceof Installments)) {
            return false;
        }
        Installments other = (Installments) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.Installments[ id=" + id + " ]";
    }
    
}
