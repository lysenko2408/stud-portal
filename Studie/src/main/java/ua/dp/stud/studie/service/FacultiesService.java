package ua.dp.stud.studie.service;

import ua.dp.stud.studie.model.Faculties;

import java.util.List;

/**
 * @author: Pikus Vladislav
 */
public interface FacultiesService {
    void saveList(List<Faculties> facList);

    void deleteList(List<Faculties> facList);
}
