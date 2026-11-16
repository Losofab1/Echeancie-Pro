/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import entities.ClientCreationRequests;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author fabri
 */
@Stateless
public class ClientCreationRequestsFacade extends AbstractFacade<ClientCreationRequests> implements ClientCreationRequestsFacadeLocal {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ClientCreationRequestsFacade() {
        super(ClientCreationRequests.class);
    }
    
}
