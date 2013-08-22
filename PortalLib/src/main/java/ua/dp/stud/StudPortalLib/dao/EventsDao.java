/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.dp.stud.StudPortalLib.dao;

import java.util.Collection;
import java.util.List;
import ua.dp.stud.StudPortalLib.model.Events;
import ua.dp.stud.StudPortalLib.model.Tags;
import ua.dp.stud.StudPortalLib.util.EventsType;

/**
 *
 * @author Ольга
 */
public interface EventsDao extends DaoForApprove<Events> {

    Collection<Events> getEventsByType(EventsType type);

    Collection<Events> getEventsOfTypeOnPage(Integer pageNumb, Integer orgsPerPage, String type, Boolean approve);

    Collection<Events> getAllEvents(Boolean approved);
    
    Events getEventsByName(String title);

    Collection<Events> getOnMainPage();
    
    void incrementViews(Events event);
    
    Integer getCountOfType(EventsType type);
    
    Events getEventsById(Integer id);
    
    Events save(Events event,List<Tags> tags);
    
    @Override
    Collection<Events> getObjectOnPage(Boolean approved, Integer pageNumb, Integer objByPage);
        
}
