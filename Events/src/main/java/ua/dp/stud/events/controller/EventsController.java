/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.dp.stud.events.controller;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import ua.dp.stud.StudPortalLib.model.Events;
import ua.dp.stud.StudPortalLib.model.ImageImpl;
import ua.dp.stud.StudPortalLib.service.EventsService;
import ua.dp.stud.StudPortalLib.util.EventsType;
import ua.dp.stud.StudPortalLib.util.ImageService;

/**
 *
 * @author Ольга
 */
@Controller
@RequestMapping(value = "view")
public class EventsController {

    private static final String MAIN_IMAGE = "mainImage";
    private static final String ADMINISTRATOR_ROLE = "Administrator";
    private static final String USER_ROLE = "User";
    private static final String STR_FAIL = "fail";
    private static final String STR_NO_IMAGE = "no images";
    private static final String STR_BAD_IMAGE = "Failed to load image";
    private static final String STR_EXEPT = "Exception";
    private static final String STR_DUPLICAT_TOPIC = "duplicating topic";
    private static final Logger LOG = Logger.getLogger(EventsController.class.getName());
    @Autowired
    @Qualifier(value = "eventsService")
    private EventsService eventsService;
    @Autowired
    @Qualifier(value = "imageService")
    private ImageService imageService;

    @RenderMapping(params = "eventId")
    public ModelAndView showSelectedEvents(RenderRequest request, RenderResponse response) throws SystemException, PortalException {
        ModelAndView model = new ModelAndView();
        int eventsID = Integer.valueOf(request.getParameter("eventsID"));
        Events event = eventsService.getEventsById(eventsID);
        ImageImpl mImage = event.getMainImage();
        String mainImageUrl = imageService.getPathToLargeImage(mImage, event);
        Collection<ImageImpl> additionalImages = event.getAdditionalImages();
        model.setView("viewSingle");
        model.addObject("additionalImages", additionalImages);
        model.addObject("mainImageUrl", mainImageUrl);
        model.addObject("event", event);
        return model;
    }

    @ModelAttribute("event")
    public Events getCommandObject() {
        return new Events();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("mainImage");
    }

    public Boolean updateEventsFields(Events event, CommonsMultipartFile mainImage, CommonsMultipartFile[] images, String frmRole, String role) {
        event.setAuthor(role);
        if (frmRole.equals(ADMINISTRATOR_ROLE)) {
            event.setApproved(true);
        }
        try {
            if (mainImage.getSize() > 0) {
                imageService.saveMainImage(mainImage, event);
            }
            if (images != null && images.length > 0) {
                for (CommonsMultipartFile file : images) {
                    imageService.saveAdditionalImages(file, event);
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, STR_EXEPT, ex);
            return false;
        }
//success upload message
        return true;
    }

    @InitBinder("event")
    @ActionMapping(value = "addEvents")
    public void addEvent(@ModelAttribute(value = "event") @Valid Events event,
            BindingResult bindingResult,
            ActionRequest actionRequest,
            ActionResponse actionResponse, SessionStatus sessionStatus, @RequestParam(MAIN_IMAGE) CommonsMultipartFile mainImage,
            @RequestParam("images") CommonsMultipartFile[] images)
            throws SystemException, PortalException {
        if (bindingResult.hasFieldErrors()) {
            actionResponse.setRenderParameter(STR_FAIL, " ");
        } else {
//path for main image is not empty
            if (mainImage.getOriginalFilename().equals("")) {
                actionResponse.setRenderParameter(STR_FAIL, STR_NO_IMAGE);
            }
//getting all parameters from form
//            EventsType type = EventsType.valueOf(actionRequest.getParameter("type"));
//crop main image
            CommonsMultipartFile croppedImage = imageService.cropImage(mainImage, Integer.parseInt(actionRequest.getParameter("t")),
                    Integer.parseInt(actionRequest.getParameter("l")),
                    Integer.parseInt(actionRequest.getParameter("w")),
                    Integer.parseInt(actionRequest.getParameter("h")));
            if (croppedImage == null) {
                actionResponse.setRenderParameter(STR_FAIL, STR_BAD_IMAGE);
                return;
            }
//check the uniqueness of the name
            String role;
            role = actionRequest.isUserInRole(ADMINISTRATOR_ROLE) ? ADMINISTRATOR_ROLE : USER_ROLE;
            User user = (User) actionRequest.getAttribute(WebKeys.USER);
            String usRole = user.getScreenName();
//try to update fields for new organisation
            if (!eventsService.isUnique(event)) {
                if (updateEventsFields(event, mainImage, images, role, usRole)) {
                    Date date = new Date();
                    event.setPublication(date);
                    eventsService.addEvents(event);
                    actionResponse.setRenderParameter("eventID", Integer.toString(event.getId()));
                    sessionStatus.setComplete();
                }
            } else {
                actionResponse.setRenderParameter(STR_FAIL, STR_DUPLICAT_TOPIC);
            }

        }
    }

    @ActionMapping(value = "editEvent")
    public void editEvent(@RequestParam(MAIN_IMAGE) CommonsMultipartFile mainImage,
            @RequestParam("images") CommonsMultipartFile[] images, @ModelAttribute(value = "event") @Valid Events event,
            BindingResult bindingResult,
            ActionRequest actionRequest,
            ActionResponse actionResponse, SessionStatus sessionStatus)
            throws IOException, SystemException, PortalException {
//getting current news
        int eventID = Integer.valueOf(actionRequest.getParameter("eventId"));
        Events newEvent = eventsService.getEventsById(eventID);
//getting all parameters from form
        if (bindingResult.hasFieldErrors()) {
            actionResponse.setRenderParameter(STR_FAIL, " ");
        } else {
            CommonsMultipartFile croppedImage;
            if (!actionRequest.getParameter("t").equals("")) {
                croppedImage = imageService.cropImage(mainImage, Integer.parseInt(actionRequest.getParameter("t")),
                        Integer.parseInt(actionRequest.getParameter("l")),
                        Integer.parseInt(actionRequest.getParameter("w")),
                        Integer.parseInt(actionRequest.getParameter("h")));
            } else {
                croppedImage = mainImage;
            }
            if (croppedImage == null) {
                actionResponse.setRenderParameter(STR_FAIL, STR_BAD_IMAGE);
                return;
            }
            String role;
            role = actionRequest.isUserInRole(ADMINISTRATOR_ROLE) ? ADMINISTRATOR_ROLE : USER_ROLE;
            User user = (User) actionRequest.getAttribute(WebKeys.USER);
            String usRole = user.getScreenName();
            if (updateEventsFields(event, mainImage, images, role, usRole)) {
                eventsService.updateEvents(event);
//close session
                sessionStatus.setComplete();
            } else {
                actionResponse.setRenderParameter(STR_FAIL, STR_DUPLICAT_TOPIC);
            }
        }
    }
    
     @RenderMapping(params = "mode=add")
    public ModelAndView showAddOrgs(RenderRequest request, RenderResponse response) {
        ModelAndView model = new ModelAndView();
//set view for add
        model.setViewName("addOrganisation");
        return model;
    }

    @RenderMapping(params = "mode=delImage")
    public ModelAndView delImage(RenderRequest request, RenderResponse response) {
        long imageID = Long.valueOf(request.getParameter("imageId"));
        ImageImpl image = eventsService.getImageById(imageID);
//delete image from folder
        imageService.deleteImage(image, image.getBase());
//delete image from data base
        eventsService.deleteImage(image);
        return showAddSuccess(request, response);
    }

    @RenderMapping(params = "mode=edit")
    public ModelAndView showEditEvent(RenderRequest request, RenderResponse response) {
        ModelAndView model = new ModelAndView();
//getting event
        int eventID = Integer.valueOf(request.getParameter("eventId"));
        Events event = eventsService.getEventsById(eventID);
        ImageImpl mImage = event.getMainImage();
        String mainImageUrl;
        mainImageUrl = imageService.getPathToLargeImage(mImage, event);
        Collection<ImageImpl> additionalImages = event.getAdditionalImages();
//set view for edit
        model.setViewName("editEvent");
//send current event in view
        model.addObject("event", event);
        model.addObject(MAIN_IMAGE, mainImageUrl);
        model.addObject("additionalImages", additionalImages);
        return model;
    }

    @RenderMapping(params = "mode=delete")
    public ModelAndView deleteOrganisation(RenderRequest request, RenderResponse response) {
//getting current events
        int eventID = Integer.valueOf(request.getParameter("eventId"));
        Events event = eventsService.getEventsById(eventID);
//delete chosen organization's image from folder
        imageService.deleteDirectory(event);
//delete chosen news
        eventsService.deleteEvents(event);
        return showAddSuccess(request, response);
    }

    @RenderMapping(params = "success")
    public ModelAndView showAddSuccess(RenderRequest request, RenderResponse response) {
        ModelAndView model = showView(request, response);
        String strSuccess = "success";
        SessionMessages.add(request, request.getParameter(strSuccess));
        return model;
    }

    @RenderMapping(params = "fail")
    public ModelAndView showAddFailed(RenderRequest request, RenderResponse response) {
        ModelAndView model = showAddOrgs(request, response);
        return model;
    }

    @RenderMapping(params = "exception")
    public ModelAndView showAddException(RenderRequest request, RenderResponse response) {
        ModelAndView model = showAddOrgs(request, response);
        model.addObject(STR_EXEPT, request.getParameter(STR_EXEPT));
        return model;
    }
}