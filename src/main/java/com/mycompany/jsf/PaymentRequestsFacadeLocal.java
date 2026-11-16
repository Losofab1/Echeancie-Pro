/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.PaymentRequests;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface PaymentRequestsFacadeLocal {

    void create(PaymentRequests paymentRequests);

    void edit(PaymentRequests paymentRequests);

    void remove(PaymentRequests paymentRequests);

    PaymentRequests find(Object id);

    List<PaymentRequests> findAll();

    List<PaymentRequests> findRange(int[] range);

    int count();
    
}
