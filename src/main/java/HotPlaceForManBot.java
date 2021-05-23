import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class HotPlaceForManBot extends TelegramLongPollingBot {
    private Update update;
    private String closedGroupChatId;
    private String adminChatId;
    //private String inviteLink;   rework invite link system with counter group users
    private int adminID;
    private AnswerSwitcher answerSwitcher = new AnswerSwitcher();

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new HotPlaceForManBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        this.update = update;
        System.out.println(answerSwitcher.getAnswerChatId() + answerSwitcher.getSwitcherStatement());
        answerFromAdminHandler();
        callBackQueryChecker();
        photoFilter();
        setAdminChatId();			// вынести в отдельные классы
        setClosedGroupChatId();
        router();
        System.out.println("\niteration finished !\n");
    }

    public void router() {
        switch (update.getMessage().getText()) {
            case "/start", "Back" -> {
                answerFactory("Привецтвую, я бот по контролю доступа к закрытой групе." +
												 "Вы можете использовать клавиши для навигации, либо команду /help что бы узнать и использовать команды в ручную", false, "Получить доступ (500р)");
            }
            case "Получить доступ (500р)" -> {
                answerFactory("Выберите удобную для вас платежную систему", false, "PayPal", "Peeyer", "Skrill", "Back");
            }
            case "PayPal" -> {
                answerFactory("Проследуйте по ссылке и оплатите подписку \npaypal.me/insidercat \nВАЖНО! После оплаты отправьте скриншот(нажать кнопку printscrine на клавиатуре на экране с подтверждением оплаты, затем CTRL+V в этот чат с ботом) с оплатой в этот чат и нажмите кнопку \" Оплачено \".", false, "Оплачено", "Back");
            }
            case "Peeyer" -> {
                answerFactory("""
                        Проследуйте по ссылке и оплатите подписку\s
                        PEEYER:
                        P1044170240
                        https://payeer.com/ru/\s
                        ВАЖНО! После оплаты отправьте скриншот(нажать кнопку printscrine на клавиатуре на экране с подтверждением оплаты, затем CTRL+V в этот чат с ботом) с оплатой в этот чат и нажмите кнопку " Оплачено ".""", false, "Оплачено", "Back");
            }
            case "Skrill" -> {
                answerFactory("Проследуйте по ссылке и оплатите подписку\nРеквизиты: insidercat@protonmail.com \nhttps://www.skrill.com/ \nВАЖНО! После оплаты отправьте скриншот(нажать кнопку printscrine на клавиатуре на экране с подтверждением оплаты, затем CTRL+V в этот чат с ботом) с оплатой в этот чат и нажмите кнопку \" Оплачено \".", false, "Оплачено", "Back");
            }
            case "Оплачено" -> {
                answerFactory("Теперь осталось только подождать подтверждения оплаты. \nЭто не займет много времени.\nСразу же после дотверждения вы будете допущены к ресурсу.", false, "Повторить отправку", "Back");
            }
            case "/clean" -> {
                cleaner();
            }
        }
    }

    
    
    public void callBackQueryChecker() { // reaction on admins buttons from photo
        if (update.hasCallbackQuery()) {
            String[] buttonContain = update.getCallbackQuery().getData().split(" "); // line choicer
            switch (buttonContain[0]) {
                case "Approve" -> {
                    approve(buttonContain[1]);
                }
                case "Answer" -> {
                    System.out.println("In answ");
                    answerSwitcher.setSwitcherStatement(true);
                    answerSwitcher.setAnswerChatId(buttonContain[1]);
                    System.out.println("Switcher filled");

                    try {
                        SendMessage message = new SendMessage();
                        message.setText("Waiting for text");
                        message.setChatId(adminChatId);
                        execute(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("answ finished");
                }
            }
        }
    }

    public void photoFilter() {
        if (update.getMessage().hasPhoto()) {
            SendPhoto result = new SendPhoto();
            result.setPhoto(new InputFile().setMedia(update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId()));
            result.setChatId(adminChatId);
            result.setReplyMarkup(adminDecision()); //<=== ex admin decision (buffering ID)
            try {
                execute(result);
                System.out.println("executing from photo");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                System.out.println("crushed in filter");
            }
        }
    }

    public void approve(String targetChatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttonsList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton("Вход");
        firstButton.setUrl(getNewLink());

        row.add(firstButton);
        buttonsList.add(row);
        keyboard.setKeyboard(buttonsList);

        SendMessage message = new SendMessage();
        message.setChatId(targetChatId);
        message.setText("Добро пожаловать");
        message.enableMarkdown(true);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.out.println("Crushed in approve");
        }
        System.out.println("approve finished");
    }

    public void answerFromAdminHandler() {
        if (answerSwitcher.getSwitcherStatement() && update.getMessage().getChatId().toString().equals(adminChatId)) {
            System.out.println("In answer");
            SendMessage messageFromAdmin = new SendMessage();
            SendMessage messageForAdmin = new SendMessage();

            messageFromAdmin.setChatId(answerSwitcher.getAnswerChatId());
            messageFromAdmin.setText(update.getMessage().getText());

            messageForAdmin.setChatId(adminChatId);
            messageForAdmin.setText("Answer has been send");

            try {
                execute(messageFromAdmin);
                execute(messageForAdmin);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("crushed in answ from admin");
            }
            answerSwitcher.setSwitcherStatement(false);

        }
    }

    public String getNewLink() {
        ExportChatInviteLink linkRequest = new ExportChatInviteLink();
        linkRequest.setChatId(closedGroupChatId);
        String link = "not initialised link";
        try {
            link = execute(linkRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.out.println("Crushed in get link");
        }
        System.out.println("link created == " + link);
        return link;
    }

    private InlineKeyboardMarkup adminDecision() {
        //CallbackQuery query = new CallbackQuery(); query.setData(update.getMessage().getChatId().toString());
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttonsList = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton("Approve");
        firstButton.setCallbackData("Approve " + update.getMessage().getChatId().toString());
        InlineKeyboardButton secondButton = new InlineKeyboardButton("Answer");
        secondButton.setCallbackData("Answer " + update.getMessage().getChatId().toString());

        row.add(firstButton);
        row.add(secondButton);
        buttonsList.add(row);
        keyboard.setKeyboard(buttonsList);
        return keyboard;
    }
    

    public void answerFactory(String msgText, boolean isAnswer, String... buttonsText) {
        SendMessage result = new SendMessage();
        result.enableMarkdown(true);
        result.setChatId(update.getMessage().getChatId().toString());
        result.setText(msgText);
        if (isAnswer) result.setReplyToMessageId(update.getMessage().getMessageId());
        if (buttonsText.length != 0) {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setSelective(true);
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(false);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            for (int i = 0; i < (buttonsText.length / 2) + 1; i++) {
                keyboardRowList.add(new KeyboardRow());
            }
            for (int i = 0, j = 0; i < buttonsText.length; i++) {
                if (i % 2 == 0 && i != 0) {
                    j++;
                }
                keyboardRowList.get(j).add(new KeyboardButton(buttonsText[i]));
            }
            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            result.setReplyMarkup(replyKeyboardMarkup);
        }
        try {
            execute(result);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void setAdminChatId() {
        if (update.getMessage().getText().equals("/setMeAsAdmin")) {
            this.adminChatId = update.getMessage().getChatId().toString();
            this.adminID = update.getMessage().getFrom().getId();
            answerFactory("You my boss now", true);
            System.out.println("Admin was bounded");
        }
    }

    public void setClosedGroupChatId() {
        if (update.getMessage().getText().equals("/setGroup") && update.getMessage().getFrom().getId() == adminID) {
            this.closedGroupChatId = update.getMessage().getChatId().toString();
            SendMessage message = new SendMessage();
            message.setChatId(adminChatId);
            message.setText("Chat was bounded");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    
	public void cleaner(){
		SendMessage cleaner = new SendMessage();
		cleaner.setChatId(update.getMessage().getChatId().toString());
		cleaner.setText("cleaned");
		cleaner.enableMarkdown(true);
		
		ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
		keyboardMarkup.setOneTimeKeyboard(true);
		List<KeyboardRow> list = new ArrayList<KeyboardRow>();
		KeyboardRow keyboardRow = new KeyboardRow();
		list.add(keyboardRow);
		keyboardRow.add(new KeyboardButton("Done"));
		keyboardMarkup.setKeyboard(list);
		
		cleaner.setReplyMarkup(keyboardMarkup);
		try {
			execute(cleaner);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

    @Override
    public String getBotUsername() {
        return "HotPlaceForManBot";
    }

    @Override
    public String getBotToken() {
        return "1618308938:AAGuBCAVDhEbztesMgRTF_U3rQdJm_xGbDI";
    }

}
