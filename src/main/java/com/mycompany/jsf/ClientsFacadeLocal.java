/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.Clients;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface ClientsFacadeLocal {

    void create(Clients clients);

    void edit(Clients clients);

    void remove(Clients clients);

    Clients find(Object id);

    List<Clients> findAll();

    List<Clients> findRange(int[] range);

    int count();
    
}
