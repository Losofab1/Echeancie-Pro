/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import java.io.Serializable;
import java.util.UUID;

import entities.Users;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;

/**
 *
 * @author fabri
 */
public class UsersConverter implements Converter, Serializable {

    private static final long serialVersionUID = 1L;

    public Object getAsObject(FacesContext facesContext, UIComponent component, String string) {
        if (string == null || string.length() == 0) {
            return null;
        }
        UUID id = UUID.fromString(string);
        UsersController controller = (UsersController) facesContext.getApplication().getELResolver().getValue(facesContext.getELContext(), null, "users");
        return controller.getJpaController().findUsers(id);
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Users) {
            Users o = (Users) object;
            return o.getId() == null ? "" : o.getId().toString();
        } else {
            throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: entities.Users");
        }
    }
    
}
