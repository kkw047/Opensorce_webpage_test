package com.cbnu11team.team11.web;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/dos")
    public List<String> dos() {
        return em.createQuery(
                "select distinct r.regionDo from RegionKor r order by r.regionDo", String.class
        ).getResultList();
    }

    @GetMapping("/sis")
    public List<String> sis(@RequestParam("do") String doName) {
        return em.createQuery(
                "select distinct r.regionSi from RegionKor r where r.regionDo = :doName order by r.regionSi",
                String.class
        ).setParameter("doName", doName).getResultList();
    }
}
