package com.game.gameapp.service;

import com.game.gameapp.exception.InformationNotFoundException;
import com.game.gameapp.model.Card;
import com.game.gameapp.model.CustomCard;
import com.game.gameapp.model.Player;
import com.game.gameapp.model.Prompt;
import com.game.gameapp.repository.CardRepository;
import com.game.gameapp.repository.CustomCardRepository;
import com.game.gameapp.repository.PlayerRepository;
import com.game.gameapp.repository.PromptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;


@Service
public class GameService {
    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());
    private static ArrayList<Card> deck;
    private static ArrayList<Prompt> prompts;
    private static final Random RNG = new Random();
    private PlayerRepository playerRepository;
    private PromptRepository promptRepository;
    private CardRepository cardRepository;
    private CustomCardRepository customCardRepository;

    @Autowired
    public void setCustomCardRepository(CustomCardRepository customCardRepository) {
        this.customCardRepository = customCardRepository;
    }

    @Autowired
    public void setPlayerRepository(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Autowired
    public void setCardRepository(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Autowired
    public void setPromptRepository(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    public ArrayList<Player> newGame(List<Long> playerIds) {
        LOGGER.info("Calling newGame method from game service.");
        ArrayList<Player> currentPlayers = new ArrayList<>();
        for (Long playerId : playerIds) {
            Optional<Player> player = playerRepository.findById(playerId);
            // Check that player exists
            if (player.isPresent()) {
                currentPlayers.add(player.get());
                LOGGER.info("Player " + player.get().getName() + " w/ id " + player.get().getId()
                        + " added to the game.");
                // set hand to empty
                player.get().setHand(new ArrayList<>());
                // set initial score to 0
                player.get().setScore(0);
            } else {
                throw new InformationNotFoundException("Please add only valid players and try again.");
            }
        }
        return currentPlayers;
    }

    public void drawUpToTen(Long playerId) {
        Optional<Player> player = playerRepository.findById(playerId);
        if (player.isPresent() && player.get().getHand().size()<10) {
            do {
                int n = RNG.nextInt(deck.size());
                Card card = deck.get(n);
                player.get().setCard(card);
                deck.remove(n);
            } while(player.get().hand.size()<10);
        }
    }

    public ArrayList<Card> createDeck() {
        LOGGER.info("Calling createDeck from game service.");
        ArrayList<Card> cards = (ArrayList<Card>) cardRepository.findAll();
//        List<CustomCard> customCards = customCardRepository.findAll();
        if (cards.isEmpty()) {
            throw new InformationNotFoundException("Please ensure the cards are loaded into the card table.");
        } else {
//            List<Object> newList = new ArrayList<>();
//            newList.add(cards);
//            newList.add(customCards);
            return cards;
        }
    }

    public ArrayList<Prompt> createPrompts() {
        LOGGER.info("Calling createPrompts from game service.");
       ArrayList<Prompt> prompts = (ArrayList<Prompt>) promptRepository.findAll();
        if (prompts.isEmpty()) {
            throw new InformationNotFoundException("Please ensure the prompts are loaded into the prompt table.");
        } else {
            return prompts;
        }
    }

    public Player firstJudge(List<Player> currentPlayers) {
        int index = RNG.nextInt(currentPlayers.size());
        Player judge = currentPlayers.get(index);
        LOGGER.info("The first judge is " + judge.getName());
        return judge;
    }

    public Prompt drawPrompt() {
        LOGGER.info("Calling drawPrompts from game service.");
        int n = RNG.nextInt(prompts.size());
        Prompt prompt = prompts.get(n);
        prompts.remove(n);
        return prompt;
    }

    public LinkedHashMap<Card,Player> getResponses(List<Player> currentPlayers, Player judge) {
        LinkedHashMap<Card, Player> responses = new LinkedHashMap<>();
        // loop all players for this round
        for(Player player : currentPlayers) {
            drawUpToTen(player.getId()); // keeps all players at max hand size
            // if player is not the judge this round
            if(!player.equals(judge)) {
                Card card = player.hand.get(RNG.nextInt(10));
                responses.put(card, player);
                LOGGER.info(player.getName() + " played " + card.getText());
            }
        }
        return responses;
    }

    public Player getWinner(LinkedHashMap<Card,Player> responses) {
        // judge picks winning response
        int n = RNG.nextInt(responses.size());
        // Get keys from linkedHashMap
        List<Card> respKeyList = new ArrayList<>(responses.keySet());
        // Get winning card using index n
        Card bestResponse = respKeyList.get(n);
        Player winner = responses.get(bestResponse);
        LOGGER.info(winner.getName()+" played "+ bestResponse+" this round!");
        return winner;
    }

    public int checkScores(Player winner, int topScore) {
        // winning player score +1
        winner.setScore(winner.getScore()+1);
        // if player score > topScore
        if (winner.getScore() > topScore) {
            // topScore = player score
            topScore = winner.getScore();
        }
        return topScore;
    }

    public Player nextJudge(Player thisJudge, ArrayList<Player> currentPlayers) {
        // find index of next judge
        int nextJudge = (currentPlayers.indexOf(thisJudge)+1);
        // if last player in list was the judge
        if (nextJudge == currentPlayers.size()) {
            // the next judge will be first player
            nextJudge = 0;
        }
        return currentPlayers.get(nextJudge);
    }

    public void playGame(LinkedHashMap<String, ArrayList<Long>> players) {
        LOGGER.info("Calling playGame method from game service.");
        // Saves playerIds from HashMap to ArrayList
        ArrayList<Long> playerIds = players.get("players");

        // Sets up new game & checks for valid players
        ArrayList<Player> currentPlayers = newGame(playerIds);

        // create deck & prompts
        deck = createDeck();
        prompts = createPrompts();

        // use RNG to pick random first judge
        Player judge = firstJudge(currentPlayers);

        // initialize tracking variables
        int topScore = 0;
        int round = 1;

        // while topScore != 10, play game
        while(topScore != 10) {
            // draw prompt
            Prompt prompt = drawPrompt();

            LOGGER.info("Judge " + judge.getName() +" drew prompt "+ prompt.getText());

            // players play response and draw up to 10
            LinkedHashMap<Card,Player> responses = getResponses(currentPlayers, judge);

            // judge chooses the best response for the round
            Player winner = getWinner(responses);

            // score tracking is updated
            topScore = checkScores(winner, topScore);

            // If game is not over
            if (topScore != 10) {
                LOGGER.info(winner.getName()+" wins round " + round + "! Their new score is " + winner.getScore());
                // rotate next judge
                judge = nextJudge(judge, currentPlayers);
                LOGGER.info("Next judge is "+ judge.getName());
            } else {
                LOGGER.info("Game Over! "+ winner.getName() + " wins!!");
            }
            LOGGER.info("End of round "+ round + ".\n");
            // increment round tracker
            round++;
        }
    }
}