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
@Table(name = "payment_submissions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentSubmissions.findAll", query = "SELECT p FROM PaymentSubmissions p"),
    @NamedQuery(name = "PaymentSubmissions.findByAmount", query = "SELECT p FROM PaymentSubmissions p WHERE p.amount = :amount"),
    @NamedQuery(name = "PaymentSubmissions.findByChannel", query = "SELECT p FROM PaymentSubmissions p WHERE p.channel = :channel"),
    @NamedQuery(name = "PaymentSubmissions.findByReference", query = "SELECT p FROM PaymentSubmissions p WHERE p.reference = :reference"),
    @NamedQuery(name = "PaymentSubmissions.findByNote", query = "SELECT p FROM PaymentSubmissions p WHERE p.note = :note"),
    @NamedQuery(name = "PaymentSubmissions.findByStatus", query = "SELECT p FROM PaymentSubmissions p WHERE p.status = :status"),
    @NamedQuery(name = "PaymentSubmissions.findByReason", query = "SELECT p FROM PaymentSubmissions p WHERE p.reason = :reason"),
    @NamedQuery(name = "PaymentSubmissions.findBySubmittedAt", query = "SELECT p FROM PaymentSubmissions p WHERE p.submittedAt = :submittedAt"),
    @NamedQuery(name = "PaymentSubmissions.findByReviewedAt", query = "SELECT p FROM PaymentSubmissions p WHERE p.reviewedAt = :reviewedAt")})
public class PaymentSubmissions implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private UUID id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "amount")
    private long amount;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "channel")
    private String channel;
    @Size(max = 80)
    @Column(name = "reference")
    private String reference;
    @Size(max = 240)
    @Column(name = "note")
    private String note;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "status")
    private String status;
    @Size(max = 240)
    @Column(name = "reason")
    private String reason;
    @Basic(optional = false)
    @NotNull
    @Column(name = "submitted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;
    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;
    @Column(name = "payment_group_id")
    private UUID paymentGroupId;
    @Column(name = "payment_group_amount")
    private Long paymentGroupAmount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "credit_amount")
    private long creditAmount;
    @Column(name = "payment_date")
    @Temporal(TemporalType.DATE)
    private Date paymentDate;
    @Size(max = 255)
    @Column(name = "proof_file_name")
    private String proofFileName;
    @Size(max = 128)
    @Column(name = "proof_content_type")
    private String proofContentType;
    @Column(name = "proof_data", columnDefinition = "bytea")
    private byte[] proofData;
    @JoinColumn(name = "installment_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Installments installmentId;
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    @ManyToOne
    private Users reviewedBy;
    @JoinColumn(name = "submitted_by", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Users submittedBy;

    public PaymentSubmissions() {
    }

    public PaymentSubmissions(UUID id) {
        this.id = id;
    }

    public PaymentSubmissions(UUID id, long amount, String channel, String status, Date submittedAt) {
        this.id = id;
        this.amount = amount;
        this.channel = channel;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getPaymentGroupId() {
        return paymentGroupId;
    }

    public void setPaymentGroupId(UUID paymentGroupId) {
        this.paymentGroupId = paymentGroupId;
    }

    public Long getPaymentGroupAmount() {
        return paymentGroupAmount;
    }

    public void setPaymentGroupAmount(Long paymentGroupAmount) {
        this.paymentGroupAmount = paymentGroupAmount;
    }

    public long getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(long creditAmount) {
        this.creditAmount = creditAmount;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getProofFileName() {
        return proofFileName;
    }

    public void setProofFileName(String proofFileName) {
        this.proofFileName = proofFileName;
    }

    public String getProofContentType() {
        return proofContentType;
    }

    public void setProofContentType(String proofContentType) {
        this.proofContentType = proofContentType;
    }

    public byte[] getProofData() {
        return proofData;
    }

    public void setProofData(byte[] proofData) {
        this.proofData = proofData;
    }

    public Installments getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(Installments installmentId) {
        this.installmentId = installmentId;
    }

    public Users getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Users reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Users getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Users submittedBy) {
        this.submittedBy = submittedBy;
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
        if (!(object instanceof PaymentSubmissions)) {
            return false;
        }
        PaymentSubmissions other = (PaymentSubmissions) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.PaymentSubmissions[ id=" + id + " ]";
    }
    
}
