/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.FlywaySchemaHistory;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface FlywaySchemaHistoryFacadeLocal {

    void create(FlywaySchemaHistory flywaySchemaHistory);

    void edit(FlywaySchemaHistory flywaySchemaHistory);

    void remove(FlywaySchemaHistory flywaySchemaHistory);

    FlywaySchemaHistory find(Object id);

    List<FlywaySchemaHistory> findAll();

    List<FlywaySchemaHistory> findRange(int[] range);

    int count();
    
}
