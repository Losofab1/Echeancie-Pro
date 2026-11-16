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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author fabri
 */
@Entity
@Table(name = "payment_request_installments")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PaymentRequestInstallments.findAll", query = "SELECT p FROM PaymentRequestInstallments p"),
    @NamedQuery(name = "PaymentRequestInstallments.findByInstallmentNumber", query = "SELECT p FROM PaymentRequestInstallments p WHERE p.installmentNumber = :installmentNumber"),
    @NamedQuery(name = "PaymentRequestInstallments.findByDueDate", query = "SELECT p FROM PaymentRequestInstallments p WHERE p.dueDate = :dueDate")})
public class PaymentRequestInstallments implements Serializable {

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
    @Column(name = "due_date")
    @Temporal(TemporalType.DATE)
    private Date dueDate;
    @JoinColumn(name = "request_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private PaymentRequests requestId;

    public PaymentRequestInstallments() {
    }

    public PaymentRequestInstallments(UUID id) {
        this.id = id;
    }

    public PaymentRequestInstallments(UUID id, short installmentNumber, Date dueDate) {
        this.id = id;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
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

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public PaymentRequests getRequestId() {
        return requestId;
    }

    public void setRequestId(PaymentRequests requestId) {
        this.requestId = requestId;
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
        if (!(object instanceof PaymentRequestInstallments)) {
            return false;
        }
        PaymentRequestInstallments other = (PaymentRequestInstallments) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.PaymentRequestInstallments[ id=" + id + " ]";
    }
    
}
