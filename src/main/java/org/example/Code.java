package org.example;

public enum Code {
    REGISTER(1),
    REGISTERED(2),
    REGISTER_DENIED(3),
    DE_REGISTER(4),
    PUBLISH(5),
    PUBLISHED(6),
    PUBLISH_DENIED(7),
    REMOVE(8),
    REMOVED(9),
    REMOVE_DENIED(10),
    UPDATE(11),
    FILE_REQ(12),
    FILE_CONF(13),
    FILE(14),
    FILE_END(15),
    FILE_ERROR(16),
    UPDATE_CONTACT(17),
    UPDATE_CONFIRMED(18),
   //
    CONTACT_UPDATE(19),
    CONTACT_DENIED(20),
    CONTACT_CONFRIMED(21);

    // Add more message codes as needed

    private final int code;

    Code(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
