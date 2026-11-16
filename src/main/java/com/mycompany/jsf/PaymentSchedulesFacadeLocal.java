/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.PaymentSchedules;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface PaymentSchedulesFacadeLocal {

    void create(PaymentSchedules paymentSchedules);

    void edit(PaymentSchedules paymentSchedules);

    void remove(PaymentSchedules paymentSchedules);

    PaymentSchedules find(Object id);

    List<PaymentSchedules> findAll();

    List<PaymentSchedules> findRange(int[] range);

    int count();
    
}
