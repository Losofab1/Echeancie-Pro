/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.PaymentSubmissions;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface PaymentSubmissionsFacadeLocal {

    void create(PaymentSubmissions paymentSubmissions);

    void edit(PaymentSubmissions paymentSubmissions);

    void remove(PaymentSubmissions paymentSubmissions);

    PaymentSubmissions find(Object id);

    List<PaymentSubmissions> findAll();

    List<PaymentSubmissions> findRange(int[] range);

    int count();
    
}
