package com.tamali_app_back.www.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur explicite pour gérer les requêtes OPTIONS (preflight CORS)
 * Cela garantit que les requêtes preflight sont toujours traitées correctement
 * même si Spring Security ou d'autres filtres ne les gèrent pas correctement
 */
@RestController
@Slf4j
public class CorsController {

    /**
     * Gère toutes les requêtes OPTIONS pour garantir que les requêtes preflight CORS
     * sont toujours traitées correctement.
     * 
     * Note: Les annotations @CrossOrigin sont gérées par Spring, mais ce contrôleur
     * sert de filet de sécurité supplémentaire pour garantir que les requêtes OPTIONS
     * sont toujours traitées avec un code 200 OK.
     */
    @CrossOrigin(origins = {"https://tamali.vercel.app", "http://localhost:4200"}, 
                 maxAge = 3600, 
                 allowCredentials = "true")
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        log.info("Requête OPTIONS (preflight CORS) reçue et traitée explicitement");
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
