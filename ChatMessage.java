import java.io.Serializable;
/**
 * ChatMessage class
 *
 * Models a message for users to send and receive
 *
 * @author David Sillman
 *
 * @version 11/8/18
 *
 */

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;

    public enum ChatType {
        GLOBAL,
        COMMAND,
        PRIVATE,
        CONNECTION
    }

    private String message;
    private ChatType type;
    private String recipient;

    public ChatMessage() {
        this.message = null;
        this.type = null;
        this.recipient = null;
    }

    public ChatMessage(ChatType type, String message, String recipient) {
        this.type = type;
        this.message = message;
        this.recipient = recipient;
    }

    public ChatMessage(ChatType type, String message) {
        this(type, message, null);
    }

    public String getMessage() {
        return message;
    }

    public ChatType getType() {
        return type;
    }

    public String getRecipient() { return recipient; }

    @Override
    public String toString() {
        if (type == ChatType.GLOBAL || type == ChatType.PRIVATE) {
            return getMessage();
        }
        return null;
    }

}
