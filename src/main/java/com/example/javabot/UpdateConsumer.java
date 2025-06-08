package com.example.javabot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {
    private final String ROAD_MAP_SDT101 = "map_101";
    private final String ROAD_MAP_SDT104 = "map_104";
    private final String RANDOM_PICTURE  = "random_picture";

    @Value("${telegram.bot.token}")
    private String token;
    private final TelegramClient telegramClient;

    public UpdateConsumer(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }
    @PostConstruct
    public void init() {
        registerCommands(); // Register commands at startup
    }

    public void registerCommands() {
        List<BotCommand> commands = List.of(
                new BotCommand("/start", "Start the bot"),
                new BotCommand("/help", "Get help information"),
                new BotCommand("/photo", "Get a random picture")
        );

        SetMyCommands setMyCommands = new SetMyCommands(commands);

        try {
            telegramClient.execute(setMyCommands);
            System.out.println("Bot commands registered successfully.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Failed to register bot commands.");
        }
    }

    @Override
    public void consume(Update update) {
        if(update.hasMessage()){
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            System.out.println("Received message %s from %s".formatted(update.getMessage().getText(), update.getMessage().getChatId()));
            if(messageText.equals("/start"))
            {
                sendMainMenu(chatId);
            }else {
                sendMessage(chatId, "I dont understand you(((");
            }

        } else if (update.hasCallbackQuery()) {
            handleCallBackQuery(update.getCallbackQuery());
        }
    }

    private void handleCallBackQuery(CallbackQuery callbackQuery) {
        var data = callbackQuery.getData();
        var chatId = callbackQuery.getFrom().getId();
        var user = callbackQuery.getFrom();
        switch (data){
            case ROAD_MAP_SDT101 -> sendRoadMapSDT101(chatId, user);

            case ROAD_MAP_SDT104 -> sendRoadMapSDT104(chatId, user);

            case RANDOM_PICTURE -> sendRandomPicture(chatId);

            default -> sendMessage(chatId, "Unknown command");
        }
    }

    private void sendRandomPicture(Long chatId) {
        sendMessage(chatId, "Loading Picture...");
        new Thread(() -> {
            var imageUrl = "https://picsum.photos/400/300?random=1";
            try {
                URL url = new URL(imageUrl);
                var inputStream = url.openStream();
                SendPhoto sendPhoto = SendPhoto.builder().chatId(chatId)
                        .photo(new InputFile(inputStream, "random.jpg")).caption("your random picture")
                        .build();
                telegramClient.execute(sendPhoto);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void sendRoadMapSDT104(Long chatId, User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String text = "Hello\n%s\nI'm loading your road map for SDT104...".formatted(firstName + " " + lastName);
        sendMessage(chatId, text);
        sendMessage(chatId, "http://ai-maps.eu-north-1.elasticbeanstalk.com/course/sdt104/course-map");
    }

    private void sendRoadMapSDT101(Long chatId, User user) {
        String text = "Hello\n%s\nI'm loading your road map for SDT101...".formatted(user.getFirstName() + " " + user.getLastName());
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        sendMessage(chatId, text);
        sendMessage(chatId, "http://ai-maps.eu-north-1.elasticbeanstalk.com/course/sdt101/course-map");

    }

    //standart
    private void sendMessage(Long chatId, String messageText)
    {
        SendMessage message = SendMessage.builder().text(messageText).chatId(chatId).build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMainMenu(Long chatId) {
        SendMessage message = SendMessage.builder().text("Hello!!! Choose your action").chatId(chatId).build();

        var button_101 = InlineKeyboardButton.builder().text("Road map SDT 101: OOP").callbackData(ROAD_MAP_SDT101).build();

        var button_104 = InlineKeyboardButton.builder().text("Road map SDT 104: UI/UX").callbackData(ROAD_MAP_SDT104).build();

        var button_random_picture = InlineKeyboardButton.builder().text("Random picture").callbackData(RANDOM_PICTURE).build();

        List<InlineKeyboardRow> keyboardRowList = List.of(
                new InlineKeyboardRow(button_101),
                new InlineKeyboardRow(button_104),
                new InlineKeyboardRow(button_random_picture)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRowList);

        message.setReplyMarkup(markup);

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}
