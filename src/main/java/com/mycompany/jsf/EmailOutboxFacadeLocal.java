/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.jsf;

import entities.EmailOutbox;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author fabri
 */
@Local
public interface EmailOutboxFacadeLocal {

    void create(EmailOutbox emailOutbox);

    void edit(EmailOutbox emailOutbox);

    void remove(EmailOutbox emailOutbox);

    EmailOutbox find(Object id);

    List<EmailOutbox> findAll();

    List<EmailOutbox> findRange(int[] range);

    int count();
    
}
