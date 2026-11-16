/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.PaymentRequestInstallments;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface PaymentRequestInstallmentsFacadeLocal {

    void create(PaymentRequestInstallments paymentRequestInstallments);

    void edit(PaymentRequestInstallments paymentRequestInstallments);

    void remove(PaymentRequestInstallments paymentRequestInstallments);

    PaymentRequestInstallments find(Object id);

    List<PaymentRequestInstallments> findAll();

    List<PaymentRequestInstallments> findRange(int[] range);

    int count();
    
}
