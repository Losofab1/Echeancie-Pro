/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jsf;

import java.io.Serializable;

import entities.FlywaySchemaHistory;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;

/**
 *
 * @author fabri
 */
public class FlywaySchemaHistoryConverter implements Converter, Serializable {

    private static final long serialVersionUID = 1L;

    public Object getAsObject(FacesContext facesContext, UIComponent component, String string) {
        if (string == null || string.length() == 0) {
            return null;
        }
        Integer id = Integer.valueOf(string);
        FlywaySchemaHistoryController controller = (FlywaySchemaHistoryController) facesContext.getApplication().getELResolver().getValue(facesContext.getELContext(), null, "flywaySchemaHistory");
        return controller.getJpaController().findFlywaySchemaHistory(id);
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof FlywaySchemaHistory) {
            FlywaySchemaHistory o = (FlywaySchemaHistory) object;
            return o.getInstalledRank() == null ? "" : o.getInstalledRank().toString();
        } else {
            throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: entities.FlywaySchemaHistory");
        }
    }
    
}
