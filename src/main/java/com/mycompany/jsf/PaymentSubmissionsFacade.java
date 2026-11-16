/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import entities.PaymentSubmissions;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author fabri
 */
@Stateless
public class PaymentSubmissionsFacade extends AbstractFacade<PaymentSubmissions> implements PaymentSubmissionsFacadeLocal {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentSubmissionsFacade() {
        super(PaymentSubmissions.class);
    }
    
}
