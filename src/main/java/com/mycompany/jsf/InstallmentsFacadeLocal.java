/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.Installments;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface InstallmentsFacadeLocal {

    void create(Installments installments);

    void edit(Installments installments);

    void remove(Installments installments);

    Installments find(Object id);

    List<Installments> findAll();

    List<Installments> findRange(int[] range);

    int count();
    
}
