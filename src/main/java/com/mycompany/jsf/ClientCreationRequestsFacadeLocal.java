/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.ClientCreationRequests;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface ClientCreationRequestsFacadeLocal {

    void create(ClientCreationRequests clientCreationRequests);

    void edit(ClientCreationRequests clientCreationRequests);

    void remove(ClientCreationRequests clientCreationRequests);

    ClientCreationRequests find(Object id);

    List<ClientCreationRequests> findAll();

    List<ClientCreationRequests> findRange(int[] range);

    int count();
    
}
