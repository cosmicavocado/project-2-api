package com.game.gameapp.controller;

import com.game.gameapp.model.CustomCard;
import com.game.gameapp.service.CustomCardService;
import com.game.gameapp.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api")
public class CardController {
    private static final Logger LOGGER = Logger.getLogger(PlayerService.class.getName());
    private CustomCardService customCardService;

    @Autowired
    public void setCardService(CustomCardService customCardService) {
        this.customCardService = customCardService;
    }

    // http://localhost:9092/api/card
    @PostMapping(path="/card")
    public CustomCard createCard(@RequestBody CustomCard customCardObject) {
        LOGGER.info("Calling createCard method from card controller");
        return customCardService.createCard(customCardObject);
    }

    // http://localhost:9092/api/card/{cardId}
    @PutMapping(path="card/{cardId}")
    public CustomCard updateCard(@PathVariable Long cardId, @RequestBody CustomCard customCardObject) {
        LOGGER.info("Calling createCard method from card controller");
        return customCardService.updateCard(cardId, customCardObject);
    }

    // http://localhost:9092/api/card/{cardId}
    @DeleteMapping(path="/card/{cardId}")
    public String deleteCard(@PathVariable Long cardId) {
        LOGGER.info("Calling deleteCard method from card controller.");
        return customCardService.deleteCard(cardId);
    }
}
